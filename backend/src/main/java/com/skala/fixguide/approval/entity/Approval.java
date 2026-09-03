package com.skala.fixguide.approval.entity;

import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * 승인·거절 이력. 재제출 이력 보존을 위해 요청당 여러 건(append-only)으로 쌓는다
 * (API 명세서 정합성 메모 #8). 이번 브랜치에서는 안전관리자 대시보드 집계용으로만 읽는다.
 */
@Entity
@Getter
@Table(name = "approvals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Approval {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "reason_category", length = 50)
    private String reasonCategory;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    @Builder
    private Approval(
            UUID id,
            WorkRequest workRequest,
            User approver,
            ApprovalDecision decision,
            String reason,
            String reasonCategory,
            OffsetDateTime decidedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.workRequest = workRequest;
        this.approver = approver;
        this.decision = decision;
        this.reason = reason;
        this.reasonCategory = reasonCategory;
        this.decidedAt = decidedAt == null ? OffsetDateTime.now() : decidedAt;
    }
}
