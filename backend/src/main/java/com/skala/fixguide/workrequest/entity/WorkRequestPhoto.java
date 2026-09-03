package com.skala.fixguide.workrequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** ERD 3. work_request_photos — 교체 대상 제품 사진 (요청당 최대 5장) */
@Entity
@Getter
@Table(name = "work_request_photos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkRequestPhoto {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 원본 저장 경로 → originalUrl */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    /** 썸네일 경로 → thumbnailUrl (PoC 단계에서는 원본과 동일 키) */
    @Column(name = "thumbnail_key", nullable = false, length = 500)
    private String thumbnailKey;

    @Column(name = "size", nullable = false)
    private int size;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Builder
    private WorkRequestPhoto(
            UUID id,
            WorkRequest workRequest,
            String fileName,
            String storageKey,
            String thumbnailKey,
            int size,
            OffsetDateTime uploadedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.workRequest = workRequest;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.thumbnailKey = thumbnailKey;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }
}
