package com.skala.argos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** ERD 3. work_request_photos — 교체 대상 제품 사진 (요청당 최대 5장) */
@Entity
@Table(name = "work_request_photos")
@Getter
@Setter
@NoArgsConstructor
public class WorkRequestPhoto {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 원본 저장 경로 → originalUrl */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    /** 썸네일 경로 → thumbnailUrl (스캐폴딩 단계에서는 원본과 동일 키) */
    @Column(name = "thumbnail_key", nullable = false, length = 500)
    private String thumbnailKey;

    @Column(nullable = false)
    private int size;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
}
