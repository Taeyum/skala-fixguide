package com.skala.argos.service;

import com.skala.argos.common.ApiException;
import com.skala.argos.common.ErrorResponse.FieldErrorItem;
import com.skala.argos.common.KstTime;
import com.skala.argos.domain.AgentResult;
import com.skala.argos.domain.AgentRun;
import com.skala.argos.domain.ProductType;
import com.skala.argos.domain.User;
import com.skala.argos.domain.UserRole;
import com.skala.argos.domain.WorkRequest;
import com.skala.argos.domain.WorkRequestStatus;
import com.skala.argos.dto.WorkRequestDtos.ApprovalView;
import com.skala.argos.dto.WorkRequestDtos.CreateRequest;
import com.skala.argos.dto.WorkRequestDtos.CreateResponse;
import com.skala.argos.dto.WorkRequestDtos.Detail;
import com.skala.argos.dto.WorkRequestDtos.NextAction;
import com.skala.argos.dto.WorkRequestDtos.PageInfo;
import com.skala.argos.dto.WorkRequestDtos.PageResponse;
import com.skala.argos.dto.WorkRequestDtos.PatchRequest;
import com.skala.argos.dto.WorkRequestDtos.PatchResponse;
import com.skala.argos.dto.WorkRequestDtos.PhotoView;
import com.skala.argos.dto.WorkRequestDtos.RequesterView;
import com.skala.argos.dto.WorkRequestDtos.SubmitRequest;
import com.skala.argos.dto.WorkRequestDtos.SubmitResponse;
import com.skala.argos.dto.WorkRequestDtos.SummaryItem;
import com.skala.argos.repository.AgentResultRepository;
import com.skala.argos.repository.AgentRunRepository;
import com.skala.argos.repository.ApprovalRepository;
import com.skala.argos.repository.WorkRequestPhotoRepository;
import com.skala.argos.repository.WorkRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkRequestService {

    private static final DateTimeFormatter REQUEST_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> SORTABLE = Set.of("submittedAt", "createdAt", "updatedAt", "requestNo", "status");
    private static final List<WorkRequestStatus> MANAGER_VISIBLE =
            List.of(WorkRequestStatus.PENDING, WorkRequestStatus.APPROVED, WorkRequestStatus.REJECTED);

    private final WorkRequestRepository workRequestRepository;
    private final WorkRequestPhotoRepository photoRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentResultRepository agentResultRepository;
    private final ApprovalRepository approvalRepository;
    private final UserFinder userFinder;
    private final AccessPolicy accessPolicy;

    /** 5.5 POST /work-requests — draft=true면 상태만 DRAFT로 기록, 필수 검증 생략 (AC 3-6) */
    public CreateResponse create(UUID userId, CreateRequest req) {
        User user = userFinder.get(userId);
        accessPolicy.requireEngineer(user);

        if (!req.isDraft()) {
            List<FieldErrorItem> missing = missingRequired(req.equipment(), req.line(), req.substance(),
                    req.operatingCondition(), req.productName(), req.productType(), req.specJson());
            if (!missing.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                        "필수 항목이 누락되었습니다.", missing);
            }
            validateSpecSchema(req.productType(), req.specJson());
        }

        Instant now = Instant.now();
        WorkRequest wr = new WorkRequest();
        wr.setId(UUID.randomUUID());
        wr.setRequestNo(nextRequestNo());
        wr.setRequester(user);
        wr.setEquipment(req.equipment());
        wr.setLine(req.line());
        wr.setSubstance(req.substance());
        wr.setOperatingCondition(req.operatingCondition());
        wr.setProductName(req.productName());
        wr.setProductType(req.productType());
        wr.setSpecJson(req.specJson());
        wr.setSymptom(req.symptom());
        wr.setSiteMemo(req.siteMemo());
        wr.setStatus(WorkRequestStatus.DRAFT);
        wr.setCreatedAt(now);
        wr.setUpdatedAt(now);
        workRequestRepository.save(wr);

        return new CreateResponse(wr.getId(), wr.getRequestNo(), wr.getStatus(), KstTime.of(now));
    }

    /** 5.6 GET /work-requests — ENGINEER는 본인 것만, SAFETY_MANAGER는 PENDING 이상만 */
    @Transactional(readOnly = true)
    public PageResponse list(UUID userId, String statusCsv, int page, int size, String sort) {
        User user = userFinder.get(userId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), parseSort(sort));
        List<WorkRequestStatus> statuses = parseStatuses(statusCsv);

        Page<WorkRequest> result;
        if (user.getRole() == UserRole.ENGINEER) {
            result = statuses == null
                    ? workRequestRepository.findByRequesterId(user.getId(), pageable)
                    : workRequestRepository.findByRequesterIdAndStatusIn(user.getId(), statuses, pageable);
        } else {
            List<WorkRequestStatus> visible = statuses == null
                    ? MANAGER_VISIBLE
                    : statuses.stream().filter(WorkRequestStatus::visibleToManager).toList();
            result = workRequestRepository.findByStatusIn(visible, pageable);
        }

        List<SummaryItem> content = result.getContent().stream().map(wr -> toSummary(wr, user)).toList();
        return new PageResponse(content,
                new PageInfo(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    /** 5.7 GET /work-requests/{id} — AI 결과·승인 이력 포함 스냅샷 */
    @Transactional(readOnly = true)
    public Detail detail(UUID userId, UUID workRequestId) {
        User user = userFinder.get(userId);
        WorkRequest wr = getOrThrow(workRequestId);
        accessPolicy.requireReadable(user, wr);

        List<PhotoView> photos = photoRepository.findByWorkRequestIdOrderByUploadedAt(workRequestId).stream()
                .map(p -> new PhotoView(p.getId(), p.getFileName(), p.getSize(),
                        PhotoService.publicUrl(p.getThumbnailKey()), PhotoService.publicUrl(p.getStorageKey()),
                        KstTime.of(p.getUploadedAt())))
                .toList();

        Map<String, Object> agentRunView = agentRunRepository
                .findTopByWorkRequestIdOrderByStartedAtDesc(workRequestId)
                .map(run -> toAgentRunView(run, wr, user))
                .orElse(null);

        ApprovalView approvalView = approvalRepository
                .findTopByWorkRequestIdOrderByDecidedAtDesc(workRequestId)
                .map(a -> new ApprovalView(a.getDecision().name(), a.getReason(), a.getReasonCategory(),
                        a.getApprover().getName(), KstTime.of(a.getDecidedAt())))
                .orElse(null);

        return new Detail(
                wr.getId(), wr.getRequestNo(), wr.getStatus(), wr.getStatus().getLabel(),
                new RequesterView(wr.getRequester().getId(), wr.getRequester().getName()),
                wr.getEquipment(), wr.getLine(), wr.getSubstance(), wr.getOperatingCondition(),
                wr.getProductName(), wr.getProductType(),
                wr.getProductType() == null ? null : wr.getProductType().getLabel(),
                wr.getSpecJson(), wr.getSymptom(), wr.getSiteMemo(), wr.getEngineerNote(),
                photos, agentRunView, approvalView,
                KstTime.of(wr.getCreatedAt()), KstTime.of(wr.getUpdatedAt()), KstTime.of(wr.getSubmittedAt()));
    }

    /** 5.8 PATCH /work-requests/{id} — 부분 수정. PENDING·APPROVED에서는 409 IMMUTABLE_STATUS */
    public PatchResponse patch(UUID userId, UUID workRequestId, PatchRequest req) {
        User user = userFinder.get(userId);
        WorkRequest wr = getOrThrow(workRequestId);
        accessPolicy.requireOwner(user, wr);
        if (wr.getStatus().immutable()) {
            throw new ApiException(HttpStatus.CONFLICT, "IMMUTABLE_STATUS",
                    "PENDING·APPROVED 상태에서는 수정할 수 없습니다.");
        }

        if (req.equipment() != null) wr.setEquipment(req.equipment());
        if (req.line() != null) wr.setLine(req.line());
        if (req.substance() != null) wr.setSubstance(req.substance());
        if (req.operatingCondition() != null) wr.setOperatingCondition(req.operatingCondition());
        if (req.productName() != null) wr.setProductName(req.productName());
        if (req.productType() != null) wr.setProductType(req.productType());
        if (req.specJson() != null) wr.setSpecJson(req.specJson());
        if (req.symptom() != null) wr.setSymptom(req.symptom());
        if (req.siteMemo() != null) wr.setSiteMemo(req.siteMemo());
        if (req.engineerNote() != null) wr.setEngineerNote(req.engineerNote());

        if (req.productType() != null || req.specJson() != null) {
            validateSpecSchema(wr.getProductType(), wr.getSpecJson());
        }

        wr.setUpdatedAt(Instant.now());
        return new PatchResponse(wr.getId(), wr.getStatus(), KstTime.of(wr.getUpdatedAt()));
    }

    /** 5.14 PATCH /work-requests/{id}/submit-approval — 제출 전 서버 검증 실패 시 422 */
    public SubmitResponse submit(UUID userId, UUID workRequestId, SubmitRequest req) {
        User user = userFinder.get(userId);
        WorkRequest wr = getOrThrow(workRequestId);
        accessPolicy.requireOwner(user, wr);

        String note = req != null && req.engineerNote() != null && !req.engineerNote().isBlank()
                ? req.engineerNote() : wr.getEngineerNote();

        List<FieldErrorItem> errors = new ArrayList<>();
        if (wr.getStatus() != WorkRequestStatus.AI_DONE && wr.getStatus() != WorkRequestStatus.REJECTED) {
            errors.add(new FieldErrorItem("status", "AI_DONE 또는 REJECTED 상태에서만 제출할 수 있습니다."));
        }
        if (note == null || note.isBlank()) {
            errors.add(new FieldErrorItem("engineerNote", "must not be blank"));
        }
        List<AgentResult> results = agentRunRepository.findTopByWorkRequestIdOrderByStartedAtDesc(workRequestId)
                .map(AgentRun::getId)
                .map(agentResultRepository::findByRunIdOrderByAgentCode)
                .orElse(List.of());
        if (results.size() < 3) {
            errors.add(new FieldErrorItem("agentResults", "에이전트 3종(A1·A2·A3) 결과가 모두 있어야 합니다."));
        }
        results.stream()
                .filter(r -> r.getAgentCode() == com.skala.argos.domain.AgentCode.A2)
                .findFirst()
                .ifPresent(a2 -> {
                    Object items = a2.getPayloadJson().get("items");
                    if (!(items instanceof List<?> l) || l.isEmpty()) {
                        errors.add(new FieldErrorItem("agentResults.A2", "적용 법령 항목이 1건 이상 필요합니다."));
                    }
                });
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "SUBMIT_REQUIRED_FIELD_MISSING",
                    "엔지니어 설명은 제출 시 필수입니다.", errors);
        }

        Instant now = Instant.now();
        wr.setEngineerNote(note);
        wr.setStatus(WorkRequestStatus.PENDING);   // REJECTED에서 재제출 시 PENDING 복귀 (AC 6-4)
        wr.setSubmittedAt(now);
        wr.setUpdatedAt(now);
        return new SubmitResponse(wr.getId(), wr.getStatus(), KstTime.of(now));
    }

    // ---- 공용 헬퍼 (AgentService에서도 사용) ----

    public WorkRequest getOrThrow(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORK_REQUEST_NOT_FOUND",
                        "요청을 찾을 수 없습니다."));
    }

    /** 명세 5.5 Y* 필드 — draft=false일 때만 필수 */
    public static List<FieldErrorItem> missingRequired(String equipment, String line, String substance,
                                                       Map<String, Object> operatingCondition, String productName,
                                                       ProductType productType, Map<String, Object> specJson) {
        List<FieldErrorItem> errors = new ArrayList<>();
        if (isBlank(equipment)) errors.add(new FieldErrorItem("equipment", "must not be blank"));
        if (isBlank(line)) errors.add(new FieldErrorItem("line", "must not be blank"));
        if (isBlank(substance)) errors.add(new FieldErrorItem("substance", "must not be blank"));
        if (operatingCondition == null || operatingCondition.isEmpty()) {
            errors.add(new FieldErrorItem("operatingCondition", "must not be empty"));
        }
        if (isBlank(productName)) errors.add(new FieldErrorItem("productName", "must not be blank"));
        if (productType == null) errors.add(new FieldErrorItem("productType", "must not be null"));
        if (specJson == null || specJson.isEmpty()) errors.add(new FieldErrorItem("specJson", "must not be empty"));
        return errors;
    }

    /** 명세 2.3 — specJson은 productType별 필수 키 검증. 불일치 400 SPEC_SCHEMA_MISMATCH */
    public static void validateSpecSchema(ProductType productType, Map<String, Object> specJson) {
        if (productType == null || specJson == null) {
            return;
        }
        List<FieldErrorItem> missing = productType.getRequiredSpecKeys().stream()
                .filter(key -> specJson.get(key) == null || specJson.get(key).toString().isBlank())
                .map(key -> new FieldErrorItem("specJson." + key, "must not be blank"))
                .toList();
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SPEC_SCHEMA_MISMATCH",
                    "productType(" + productType + ")에 필요한 specJson 키가 누락되었습니다.", missing);
        }
    }

    // ---- private ----

    private Map<String, Object> toAgentRunView(AgentRun run, WorkRequest wr, User viewer) {
        boolean editable = viewer.getRole() == UserRole.ENGINEER
                && wr.ownedBy(viewer) && !wr.getStatus().immutable();
        List<Map<String, Object>> results = agentResultRepository.findByRunIdOrderByAgentCode(run.getId()).stream()
                .map(r -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("agentResultId", r.getId());
                    view.put("agentCode", r.getAgentCode());
                    view.put("title", r.getAgentCode().getTitle());
                    view.put("editable", editable);   // SAFETY_MANAGER 조회 시 항상 false (명세 5.7)
                    view.put("edited", r.isEdited());
                    view.putAll(r.getPayloadJson());  // items 또는 documents
                    return view;
                })
                .toList();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("runId", run.getId());
        view.put("status", run.getStatus());
        view.put("results", results);
        return view;
    }

    private SummaryItem toSummary(WorkRequest wr, User viewer) {
        return new SummaryItem(wr.getId(), wr.getRequestNo(), wr.getEquipment(), wr.getProductName(),
                wr.getProductType(), wr.getProductType() == null ? null : wr.getProductType().getLabel(),
                wr.getStatus(), wr.getStatus().getLabel(), wr.getRequester().getName(),
                KstTime.of(wr.getCreatedAt()), KstTime.of(wr.getSubmittedAt()), nextAction(wr, viewer));
    }

    /** 명세 5.6 nextAction — DRAFT→이어쓰기(E_02) · AI_RUNNING→진행(E_03) · AI_DONE→결과(E_04) · 그 외 상세 */
    private NextAction nextAction(WorkRequest wr, User viewer) {
        if (viewer.getRole() == UserRole.SAFETY_MANAGER) {
            return new NextAction(wr.getStatus() == WorkRequestStatus.PENDING ? "검토" : "상세",
                    "/manage/requests/" + wr.getId());
        }
        return switch (wr.getStatus()) {
            case DRAFT -> new NextAction("이어쓰기", "/requests/" + wr.getId() + "/edit");
            case AI_RUNNING -> new NextAction("진행 확인", "/requests/" + wr.getId() + "/progress");
            case AI_DONE -> new NextAction("결과 수정", "/requests/" + wr.getId() + "/result");
            default -> new NextAction("상세", "/requests/" + wr.getId());
        };
    }

    private String nextRequestNo() {
        String prefix = "WR-" + LocalDate.now(KstTime.KST).format(REQUEST_NO_DATE) + "-";
        int next = workRequestRepository.findTopByRequestNoStartingWithOrderByRequestNoDesc(prefix)
                .map(w -> Integer.parseInt(w.getRequestNo().substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%03d", next);
    }

    private Sort parseSort(String sort) {
        String[] parts = (sort == null || sort.isBlank() ? "submittedAt,desc" : sort).split(",");
        String field = SORTABLE.contains(parts[0].trim()) ? parts[0].trim() : "submittedAt";
        Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    private List<WorkRequestStatus> parseStatuses(String statusCsv) {
        if (statusCsv == null || statusCsv.isBlank()) {
            return null;
        }
        try {
            return Arrays.stream(statusCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(WorkRequestStatus::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "status 값이 올바르지 않습니다: " + statusCsv);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
