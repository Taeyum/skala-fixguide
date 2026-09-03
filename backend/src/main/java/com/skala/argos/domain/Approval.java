package com.skala.argos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** ERD 7. approvals — 승인·거절. append-only, 재제출 후 재결정 시 새 행. 최신 1건을 화면 노출 */
@Entity
@Table(name = "approvals", indexes = {
        @Index(name = "idx_approval_wr_decided", columnList = "work_request_id, decided_at"),
        @Index(name = "idx_approval_decided", columnList = "decided_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Approval {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApprovalDecision decision;

    /** REJECT 시 필수(10자 이상). 요청자(E_05)에게 그대로 전달 */
    @Column(columnDefinition = "text")
    private String reason;

    /** S_01 거절 사유 TOP5 집계 키 */
    @Column(name = "reason_category", length = 30)
    private String reasonCategory;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
}
