package com.skala.fixguide.workrequest.service;

import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import org.springframework.stereotype.Component;

/** API 명세서 1장 권한: ENGINEER 는 본인 요청만 조회·수정, SAFETY_MANAGER 는 PENDING 이상 전체. 위반 시 403 */
@Component
public class WorkRequestAccessPolicy {

    public void requireEngineer(AuthenticatedUser me) {
        if (me.role() != Role.ENGINEER) {
            throw new ApiException(ErrorCode.FORBIDDEN_ROLE, "엔지니어만 수행할 수 있습니다.");
        }
    }

    public void requireManager(AuthenticatedUser me) {
        if (me.role() != Role.SAFETY_MANAGER) {
            throw new ApiException(ErrorCode.FORBIDDEN_ROLE, "안전관리자만 수행할 수 있습니다.");
        }
    }

    public void requireOwner(AuthenticatedUser me, WorkRequest workRequest) {
        requireEngineer(me);
        if (!workRequest.isOwnedBy(me.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN_NOT_OWNER, "본인 요청만 접근할 수 있습니다.");
        }
    }

    public void requireReadable(AuthenticatedUser me, WorkRequest workRequest) {
        if (me.role() == Role.ENGINEER) {
            if (!workRequest.isOwnedBy(me.userId())) {
                throw new ApiException(ErrorCode.FORBIDDEN_NOT_OWNER);
            }
            return;
        }
        if (!workRequest.getStatus().visibleToSafetyManager()) {
            throw new ApiException(ErrorCode.FORBIDDEN_NOT_OWNER,
                    "안전관리자는 제출된(PENDING 이상) 요청만 조회할 수 있습니다.");
        }
    }

    /** 상세 화면 AI 결과 편집 가능 여부 — 안전관리자 조회 시 항상 false (명세 5.7) */
    public boolean canEditResults(AuthenticatedUser me, WorkRequest workRequest) {
        return me.role() == Role.ENGINEER
                && workRequest.isOwnedBy(me.userId())
                && !workRequest.getStatus().isImmutable();
    }
}
