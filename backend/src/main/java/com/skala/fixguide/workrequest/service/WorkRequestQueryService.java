package com.skala.fixguide.workrequest.service;

import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.dto.PageResponse;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.workrequest.dto.WorkRequestSummaryResponse;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메인화면(E_01 · S_01)과 내 요청 목록(E_05)이 쓰는 조회 전용 서비스.
 * 등록·수정 등 쓰기 유스케이스는 별도 서비스로 분리해 담당자가 이어받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkRequestQueryService {

    private static final Set<WorkRequestStatus> SAFETY_VISIBLE =
            EnumSet.of(WorkRequestStatus.PENDING, WorkRequestStatus.APPROVED, WorkRequestStatus.REJECTED);

    private final WorkRequestRepository workRequestRepository;

    /**
     * 요청 목록.
     *
     * <ul>
     *   <li>ENGINEER — 본인 요청만. mine=false 로 호출해도 본인 것만 반환한다(권한 규약).
     *   <li>SAFETY_MANAGER — PENDING 이후 상태만. mine=true 는 의미가 없어 403.
     * </ul>
     */
    public PageResponse<WorkRequestSummaryResponse> search(
            AuthenticatedUser me, boolean mine, String statusParam, Pageable pageable) {

        Set<WorkRequestStatus> requested = parseStatuses(statusParam);

        if (me.role() == Role.SAFETY_MANAGER) {
            if (mine) {
                throw new ApiException(
                        ErrorCode.FORBIDDEN_ROLE, "안전관리자는 mine=true 로 조회할 수 없습니다.");
            }
            Set<WorkRequestStatus> statuses = intersect(requested, SAFETY_VISIBLE);
            Page<WorkRequest> page = workRequestRepository.findByStatusIn(statuses, pageable);
            return PageResponse.of(page, entity -> WorkRequestSummaryResponse.from(entity, me.role()));
        }

        Set<WorkRequestStatus> statuses = intersect(requested, EnumSet.allOf(WorkRequestStatus.class));
        Page<WorkRequest> page =
                workRequestRepository.findByRequesterIdAndStatusIn(me.userId(), statuses, pageable);
        return PageResponse.of(page, entity -> WorkRequestSummaryResponse.from(entity, me.role()));
    }

    /** status=REJECTED,DRAFT 처럼 콤마로 여러 개 올 수 있다. 값이 없으면 전체. */
    private Set<WorkRequestStatus> parseStatuses(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return EnumSet.allOf(WorkRequestStatus.class);
        }
        List<String> tokens = Arrays.stream(statusParam.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toList();

        EnumSet<WorkRequestStatus> statuses = EnumSet.noneOf(WorkRequestStatus.class);
        for (String token : tokens) {
            try {
                statuses.add(WorkRequestStatus.valueOf(token.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ApiException(
                        ErrorCode.VALIDATION_FAILED, "지원하지 않는 status 값입니다: " + token);
            }
        }
        return statuses;
    }

    private Set<WorkRequestStatus> intersect(
            Set<WorkRequestStatus> requested, Set<WorkRequestStatus> allowed) {
        EnumSet<WorkRequestStatus> result = EnumSet.copyOf(requested);
        result.retainAll(allowed);
        return result;
    }
}
