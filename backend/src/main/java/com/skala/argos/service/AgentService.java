package com.skala.argos.service;

import com.skala.argos.common.ApiException;
import com.skala.argos.common.ErrorResponse.FieldErrorItem;
import com.skala.argos.common.KstTime;
import com.skala.argos.domain.AgentCode;
import com.skala.argos.domain.AgentResult;
import com.skala.argos.domain.AgentRun;
import com.skala.argos.domain.AgentStep;
import com.skala.argos.domain.AgentStepStatus;
import com.skala.argos.domain.RunStatus;
import com.skala.argos.domain.User;
import com.skala.argos.domain.WorkRequest;
import com.skala.argos.domain.WorkRequestStatus;
import com.skala.argos.dto.AgentDtos.PatchResultRequest;
import com.skala.argos.dto.AgentDtos.PatchResultResponse;
import com.skala.argos.dto.AgentDtos.PollResponse;
import com.skala.argos.dto.AgentDtos.StartRequest;
import com.skala.argos.dto.AgentDtos.StartResponse;
import com.skala.argos.dto.AgentDtos.StepView;
import com.skala.argos.repository.AgentResultRepository;
import com.skala.argos.repository.AgentRunRepository;
import com.skala.argos.repository.AgentStepRepository;
import com.skala.argos.repository.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    /** 명세 5.11·5.12 pollIntervalMs — 프론트 폴링 주기 2~3초 */
    private static final int POLL_INTERVAL_MS = 2500;

    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final AgentResultRepository agentResultRepository;
    private final AiConfigRepository aiConfigRepository;
    private final WorkRequestService workRequestService;
    private final UserFinder userFinder;
    private final AccessPolicy accessPolicy;
    private final MockAgentEngine engine;

    /** 5.11 POST /agent-runs — AI 검증 3종 실행 (비동기). 202 Accepted */
    public StartResponse start(UUID userId, StartRequest req) {
        User user = userFinder.get(userId);
        WorkRequest wr = workRequestService.getOrThrow(req.workRequestId());
        accessPolicy.requireOwner(user, wr);

        if (wr.getStatus().immutable()) {
            throw new ApiException(HttpStatus.CONFLICT, "IMMUTABLE_STATUS",
                    "PENDING·APPROVED 상태에서는 AI 검증을 실행할 수 없습니다.");
        }
        if (agentRunRepository.existsByWorkRequestIdAndStatus(wr.getId(), RunStatus.RUNNING)) {
            throw new ApiException(HttpStatus.CONFLICT, "RUN_ALREADY_IN_PROGRESS",
                    "동일 요청에 진행 중인 AI 검증이 있습니다.");
        }
        List<FieldErrorItem> missing = WorkRequestService.missingRequired(wr.getEquipment(), wr.getLine(),
                wr.getSubstance(), wr.getOperatingCondition(), wr.getProductName(), wr.getProductType(),
                wr.getSpecJson());
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WORK_REQUEST_INCOMPLETE",
                    "AI 검증에 필요한 요청 필수값이 누락되었습니다.", missing);
        }

        Instant now = Instant.now();
        AgentRun run = new AgentRun();
        run.setId(UUID.randomUUID());
        run.setWorkRequest(wr);
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(now);
        run.setInputSnapshot(snapshotOf(wr));   // AI 입력 원칙: 서버가 전체 스냅샷 구성 (명세 5.11)
        aiConfigRepository.findByAgentCodeAndActiveTrue(AgentCode.A1).ifPresent(run::setAiConfig);
        agentRunRepository.save(run);

        List<AgentStep> steps = new ArrayList<>();
        for (AgentCode code : AgentCode.values()) {   // PoC는 3종 전부 실행 (명세 2.4)
            AgentStep step = new AgentStep();
            step.setId(UUID.randomUUID());
            step.setRun(run);
            step.setAgentCode(code);
            step.setStatus(AgentStepStatus.WAITING);
            steps.add(step);
        }
        agentStepRepository.saveAll(steps);

        wr.setStatus(WorkRequestStatus.AI_RUNNING);
        wr.setUpdatedAt(now);

        return new StartResponse(run.getId(), wr.getId(), run.getStatus(),
                steps.stream().map(s -> toStepView(s, Map.of())).toList(), POLL_INTERVAL_MS);
    }

    /**
     * 5.12 GET /agent-runs/{runId} — 진행 상태 폴링.
     * Mock 전이(ERD 5장): 호출마다 미완료 step 하나를 DONE으로 전이하고 결과를 생성,
     * 다음 step을 RUNNING으로 표시. 3개 모두 DONE이면 allDone과 함께 요청을 AI_DONE으로 전환.
     */
    public PollResponse poll(UUID userId, UUID runId) {
        User user = userFinder.get(userId);
        AgentRun run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_RUN_NOT_FOUND",
                        "AI 실행을 찾을 수 없습니다."));
        WorkRequest wr = run.getWorkRequest();
        accessPolicy.requireReadable(user, wr);

        List<AgentStep> steps = agentStepRepository.findByRunIdOrderByAgentCode(runId);

        if (run.getStatus() == RunStatus.RUNNING) {
            advanceMock(run, wr, steps);
        }

        Map<AgentCode, UUID> resultIds = agentResultRepository.findByRunIdOrderByAgentCode(runId).stream()
                .collect(Collectors.toMap(AgentResult::getAgentCode, AgentResult::getId));
        boolean allDone = steps.stream().allMatch(s -> s.getStatus() == AgentStepStatus.DONE);

        return new PollResponse(run.getId(), wr.getId(), run.getStatus(), KstTime.of(run.getStartedAt()),
                steps.stream().map(s -> toStepView(s, resultIds)).toList(), allDone, POLL_INTERVAL_MS);
    }

    /** 5.13 PATCH /agent-results/{id} — 전체 치환(PUT-like). 배열에 없는 기존 id는 삭제, id 없이 오면 신규 추가 */
    public PatchResultResponse patchResult(UUID userId, UUID agentResultId, PatchResultRequest req) {
        User user = userFinder.get(userId);
        AgentResult result = agentResultRepository.findById(agentResultId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_RESULT_NOT_FOUND",
                        "AI 결과물을 찾을 수 없습니다."));
        WorkRequest wr = result.getRun().getWorkRequest();
        accessPolicy.requireOwner(user, wr);
        if (wr.getStatus().immutable()) {
            throw new ApiException(HttpStatus.CONFLICT, "RESULT_LOCKED",
                    "PENDING·APPROVED 상태에서는 결과를 수정할 수 없습니다.");
        }

        boolean documentType = result.getAgentCode() == AgentCode.A3;
        String key = documentType ? "documents" : "items";
        String idField = documentType ? "docId" : "itemId";
        String idPrefix = documentType ? "d-" : "i-";
        List<Map<String, Object>> incoming = documentType ? req.documents() : req.items();
        if (incoming == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    result.getAgentCode() + " 결과는 " + key + " 배열로 수정합니다.");
        }

        List<Map<String, Object>> existing = castList(result.getPayloadJson().get(key));
        Map<Object, Map<String, Object>> byId = existing.stream()
                .collect(Collectors.toMap(e -> e.get(idField), Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        int maxSeq = existing.stream()
                .map(e -> String.valueOf(e.get(idField)))
                .filter(id -> id.startsWith(idPrefix))
                .mapToInt(id -> parseSeq(id, idPrefix))
                .max().orElse(0);

        boolean anyChange = false;
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> in : incoming) {
            Map<String, Object> entry = new LinkedHashMap<>(in);
            Object id = entry.get(idField);
            Map<String, Object> old = id == null ? null : byId.get(id);
            if (old == null) {
                entry.put(idField, idPrefix + String.format("%02d", ++maxSeq));   // 신규 추가: 서버가 id 채번
                entry.put("edited", true);
                anyChange = true;
            } else {
                boolean changed = !contentEquals(old, entry, idField);
                entry.put("edited", changed || Boolean.TRUE.equals(old.get("edited")));
                if (changed) {
                    anyChange = true;
                }
            }
            merged.add(entry);
        }
        if (merged.size() != existing.size()) {   // 배열에 없는 기존 id는 삭제로 처리
            anyChange = true;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key, merged);
        result.setPayloadJson(payload);
        result.setEdited(result.isEdited() || anyChange);
        result.setUpdatedAt(Instant.now());

        return new PatchResultResponse(result.getId(), result.getAgentCode(), result.isEdited(),
                documentType ? null : merged, documentType ? merged : null, KstTime.of(result.getUpdatedAt()));
    }

    // ---- private ----

    private void advanceMock(AgentRun run, WorkRequest wr, List<AgentStep> steps) {
        Instant now = Instant.now();
        steps.stream().filter(s -> s.getStatus() != AgentStepStatus.DONE).findFirst().ifPresent(step -> {
            if (step.getStartedAt() == null) {
                step.setStartedAt(now);
            }
            step.setStatus(AgentStepStatus.DONE);
            step.setFinishedAt(now);
            step.setMessage(engine.doneMessage(step.getAgentCode(), wr));

            AgentResult result = new AgentResult();
            result.setId(UUID.randomUUID());
            result.setRun(run);
            result.setAgentCode(step.getAgentCode());
            result.setPayloadJson(engine.payload(step.getAgentCode(), wr));
            result.setOriginalJson(engine.payload(step.getAgentCode(), wr));   // AI 원본 스냅샷 보존
            result.setEdited(false);
            result.setUpdatedAt(now);
            agentResultRepository.save(result);

            steps.stream().filter(s -> s.getStatus() == AgentStepStatus.WAITING).findFirst().ifPresent(next -> {
                next.setStatus(AgentStepStatus.RUNNING);
                next.setStartedAt(now);
                next.setMessage(engine.runningMessage(next.getAgentCode()));
            });
        });

        if (steps.stream().allMatch(s -> s.getStatus() == AgentStepStatus.DONE)) {
            run.setStatus(RunStatus.DONE);
            run.setFinishedAt(now);
            wr.setStatus(WorkRequestStatus.AI_DONE);
            wr.setUpdatedAt(now);
        }
    }

    private StepView toStepView(AgentStep step, Map<AgentCode, UUID> resultIds) {
        return new StepView(step.getAgentCode(), step.getAgentCode().getTitle(), step.getStatus(),
                step.getMessage(), resultIds.get(step.getAgentCode()),
                KstTime.of(step.getStartedAt()), KstTime.of(step.getFinishedAt()));
    }

    private Map<String, Object> snapshotOf(WorkRequest wr) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workRequestId", wr.getId());
        snapshot.put("requestNo", wr.getRequestNo());
        snapshot.put("equipment", wr.getEquipment());
        snapshot.put("line", wr.getLine());
        snapshot.put("substance", wr.getSubstance());
        snapshot.put("operatingCondition", wr.getOperatingCondition());
        snapshot.put("productName", wr.getProductName());
        snapshot.put("productType", wr.getProductType());
        snapshot.put("specJson", wr.getSpecJson());
        snapshot.put("symptom", wr.getSymptom());
        snapshot.put("siteMemo", wr.getSiteMemo());
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private int parseSeq(String id, String prefix) {
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** edited·id 필드를 제외한 나머지 값이 같으면 내용 동일로 본다 */
    private boolean contentEquals(Map<String, Object> a, Map<String, Object> b, String idField) {
        Map<String, Object> left = new LinkedHashMap<>(a);
        Map<String, Object> right = new LinkedHashMap<>(b);
        for (String skip : List.of(idField, "edited")) {
            left.remove(skip);
            right.remove(skip);
        }
        return Objects.equals(left, right);
    }
}
