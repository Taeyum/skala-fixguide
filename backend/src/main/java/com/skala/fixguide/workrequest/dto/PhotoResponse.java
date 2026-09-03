package com.skala.fixguide.workrequest.dto;

import com.skala.fixguide.workrequest.entity.WorkRequestPhoto;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID photoId,
        String fileName,
        int size,
        String thumbnailUrl,
        String originalUrl,
        OffsetDateTime uploadedAt) {

    /** 업로드 파일은 /api/v1/files/** 로 정적 서빙한다 (WebConfig) */
    public static final String PUBLIC_PREFIX = "/api/v1/files/";

    public static PhotoResponse from(WorkRequestPhoto photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getFileName(),
                photo.getSize(),
                PUBLIC_PREFIX + photo.getThumbnailKey(),
                PUBLIC_PREFIX + photo.getStorageKey(),
                photo.getUploadedAt());
    }
}
