package com.skala.argos.service;

import com.skala.argos.common.ApiException;
import com.skala.argos.domain.User;
import com.skala.argos.domain.UserRole;
import com.skala.argos.domain.WorkRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 명세 1장 권한: ENGINEER는 본인 요청만 조회·수정, SAFETY_MANAGER는 PENDING 이상 전체. 위반 시 403 */
@Component
public class AccessPolicy {

    public void requireEngineer(User user) {
        if (user.getRole() != UserRole.ENGINEER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE", "엔지니어만 수행할 수 있습니다.");
        }
    }

    public void requireManager(User user) {
        if (user.getRole() != UserRole.SAFETY_MANAGER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE", "안전관리자만 수행할 수 있습니다.");
        }
    }

    public void requireOwner(User user, WorkRequest workRequest) {
        requireEngineer(user);
        if (!workRequest.ownedBy(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_NOT_OWNER", "본인 요청만 접근할 수 있습니다.");
        }
    }

    public void requireReadable(User user, WorkRequest workRequest) {
        if (user.getRole() == UserRole.ENGINEER) {
            if (!workRequest.ownedBy(user)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_NOT_OWNER", "본인 요청만 조회할 수 있습니다.");
            }
            return;
        }
        if (!workRequest.getStatus().visibleToManager()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_NOT_OWNER",
                    "안전관리자는 제출된(PENDING 이상) 요청만 조회할 수 있습니다.");
        }
    }
}
