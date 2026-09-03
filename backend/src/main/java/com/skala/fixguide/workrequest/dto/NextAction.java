package com.skala.fixguide.workrequest.dto;

import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.util.UUID;

/**
 * 상태별 다음 화면 이동 (AC 2-4 / 6-5). API 명세서 정합성 메모 #4 에 따라 서버가 계산해 내려준다.
 * FE 라우트 경로가 확정되면 이 클래스만 고치면 된다.
 */
public record NextAction(String label, String path) {

    public static NextAction of(Role viewerRole, WorkRequestStatus status, UUID workRequestId) {
        if (viewerRole == Role.SAFETY_MANAGER) {
            return new NextAction("상세", "/manage/requests/" + workRequestId);
        }
        return switch (status) {
            case DRAFT -> new NextAction("이어서", "/requests/" + workRequestId + "/edit");
            case AI_RUNNING -> new NextAction("진행", "/requests/" + workRequestId + "/progress");
            case AI_DONE -> new NextAction("결과", "/requests/" + workRequestId + "/result");
            default -> new NextAction("상세", "/requests/" + workRequestId);
        };
    }
}
