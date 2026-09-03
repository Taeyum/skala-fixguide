package com.skala.fixguide.agent.service;

import com.skala.fixguide.agent.dto.AgentResultPatchRequest;
import com.skala.fixguide.agent.dto.AgentResultPatchResponse;
import com.skala.fixguide.agent.dto.AgentRunPollResponse;
import com.skala.fixguide.agent.dto.AgentRunStartRequest;
import com.skala.fixguide.agent.dto.AgentRunStartResponse;
import com.skala.fixguide.agent.dto.AgentStepResponse;
import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.agent.entity.AgentResult;
import com.skala.fixguide.agent.entity.AgentRun;
import com.skala.fixguide.agent.entity.AgentStep;
import com.skala.fixguide.agent.entity.AgentStepStatus;
import com.skala.fixguide.agent.entity.RunStatus;
import com.skala.fixguide.agent.repository.AgentResultRepository;
import com.skala.fixguide.agent.repository.AgentRunRepository;
import com.skala.fixguide.agent.repository.AgentStepRepository;
import com.skala.fixguide.agent.repository.AiConfigRepository;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.common.error.ErrorResponse;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import com.skala.fixguide.workrequest.service.WorkRequestAccessPolicy;
import com.skala.fixguide.workrequest.service.WorkRequestValidator;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API 11~13 — AI 검증 실행 · 폴링 · 결과 수정 (Mock 상태 전이) */
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
    private final WorkRequestRepository workRequestRepository;
    private final WorkRequestAccessPolicy accessPolicy;
    private final MockAgentEngine engine;
    private final Clock clock;

    /** 5.11 POST /agent-runs — AI 검증 3종 실행. 202 Accepted */
    public AgentRunStartResponse start(AuthenticatedUser me, AgentRunStartRequest request) {
        WorkRequest wr = getWorkRequest(request.workRequestId());
        accessPolicy.requireOwner(me, wr);

        if (wr.getStatus().isImmutable()) {
            throw new ApiException(ErrorCode.IMMUTABLE_STATUS, "PENDING·APPROVED 상태에서는 AI 검증을 실행할 수 없습니다.");
        }
        if (agentRunRepository.existsByWorkRequestIdAndStatus(wr.getId(), RunStatus.RUNNING)) {
            throw new ApiException(ErrorCode.RUN_ALREADY_IN_PROGRESS, "동일 요청에 진행 중인 AI 검증이 있습니다.");
        }
        List<ErrorResponse.FieldError> missing = WorkRequestValidator.missingRequired(wr);
        if (!missing.isEmpty()) {
            throw new ApiException(ErrorCode.WORK_REQUEST_INCOMPLETE,
                    ErrorCode.WORK_REQUEST_INCOMPLETE.getDefaultMessage(), missing);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        AgentRun run = agentRunRepository.save(AgentRun.builder()
                .workRequest(wr)
                .startedAt(now)
                .inputSnapshot(snapshotOf(wr))   // AI 입력 원칙: 서버가 전체 스냅샷을 구성한다 (명세 5.11)
                .aiConfig(aiConfigRepository.findByAgentCodeAndActiveTrue(AgentCode.A1).orElse(null))
                .build());

        List<AgentStep> steps = new ArrayList<>();
        for (AgentCode code : AgentCode.values()) {   // PoC 는 3종 전부 실행 (명세 2.4)
            steps.add(AgentStep.builder().run(run).agentCode(code).build());
        }
        agentStepRepository.saveAll(steps);
        wr.startAi();

        return new AgentRunStartResponse(run.getId(), wr.getId(), run.getStatus(),
                steps.stream().map(s -> AgentStepResponse.from(s, Map.of())).toList(), POLL_INTERVAL_MS);
    }

    /**
     * 5.12 GET /agent-runs/{runId} — 진행 상태 폴링.
     * Mock 전이(ERD 5장): 호출마다 미완료 step 하나를 DONE 으로 바꾸고 결과를 생성한 뒤
     * 다음 step 을 RUNNING 으로 표시한다. 3개 모두 DONE 이면 allDone 과 함께 요청을 AI_DONE 으로 전환한다.
     */
    public AgentRunPollResponse poll(AuthenticatedUser me, UUID runId) {
        AgentRun run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new ApiException(ErrorCode.AGENT_RUN_NOT_FOUND));
        WorkRequest wr = run.getWorkRequest();
        accessPolicy.requireReadable(me, wr);

        List<AgentStep> steps = agentStepRepository.findByRunIdOrderByAgentCode(runId);
        if (run.getStatus() == RunStatus.RUNNING) {
            advanceMock(run, wr, steps);
        }

        Map<AgentCode, UUID> resultIds = agentResultRepository.findByRunIdOrderByAgentCode(runId).stream()
                .collect(Collectors.toMap(AgentResult::getAgentCode, AgentResult::getId));
        boolean allDone = steps.stream().allMatch(AgentStep::isDone);

        return new AgentRunPollResponse(run.getId(), wr.getId(), run.getStatus(), run.getStartedAt(),
                steps.stream().map(s -> AgentStepResponse.from(s, resultIds)).toList(), allDone, POLL_INTERVAL_MS);
    }

    /** 5.13 PATCH /agent-results/{id} — 전체 치환(PUT-like). 배열에 없는 기존 id 는 삭제, id 없이 오면 신규 추가 */
    public AgentResultPatchResponse patchResult(AuthenticatedUser me, UUID agentResultId, AgentResultPatchRequest request) {
        AgentResult result = agentResultRepository.findById(agentResultId)
                .orElseThrow(() -> new ApiException(ErrorCode.AGENT_RESULT_NOT_FOUND));
        WorkRequest wr = result.getRun().getWorkRequest();
        accessPolicy.requireOwner(me, wr);
        if (wr.getStatus().isImmutable()) {
            throw new ApiException(ErrorCode.RESULT_LOCKED, "PENDING·APPROVED 상태에서는 결과를 수정할 수 없습니다.");
        }

        boolean documentType = result.getAgentCode() == AgentCode.A3;
        String key = documentType ? "documents" : "items";
        String idField = documentType ? "docId" : "itemId";
        String idPrefix = documentType ? "d-" : "i-";
        List<Map<String, Object>> incoming = documentType ? request.documents() : request.items();
        if (incoming == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    result.getAgentCode() + " 결과는 " + key + " 배열로 수정합니다.");
        }

        List<Map<String, Object>> existing = castList(result.getPayloadJson().get(key));
        Map<Object, Map<String, Object>> byId = existing.stream()
                .collect(Collectors.toMap(e -> e.get(idField), Function.identity(), (a, b) -> a, LinkedHashMap::new));

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
                anyChange |= changed;
            }
            merged.add(entry);
        }
        if (merged.size() != existing.size()) {   // 배열에 없는 기존 id 는 삭제로 처리
            anyChange = true;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key, merged);
        result.replacePayload(payload, anyChange);

        return new AgentResultPatchResponse(result.getId(), result.getAgentCode(), result.isEdited(),
                documentType ? null : merged, documentType ? merged : null, OffsetDateTime.now(clock));
    }

    // ---- private ----

    private WorkRequest getWorkRequest(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.WORK_REQUEST_NOT_FOUND));
    }

    private void advanceMock(AgentRun run, WorkRequest wr, List<AgentStep> steps) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        steps.stream().filter(s -> !s.isDone()).findFirst().ifPresent(step -> {
            step.done(now, engine.doneMessage(step.getAgentCode(), wr));
            agentResultRepository.save(AgentResult.builder()
                    .run(run)
                    .agentCode(step.getAgentCode())
                    .payloadJson(engine.payload(step.getAgentCode(), wr))
                    .build());

            steps.stream()
                    .filter(s -> s.getStatus() == AgentStepStatus.WAITING)
                    .findFirst()
                    .ifPresent(next -> next.start(now, engine.runningMessage(next.getAgentCode())));
        });

        if (steps.stream().allMatch(AgentStep::isDone)) {
            run.finish(now);
            wr.finishAi();
        }
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
