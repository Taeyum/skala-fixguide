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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ERD 2. work_requests — 부품 교체 요청. 핵심 엔티티이자 상태머신의 주체.
 * DRAFT 허용을 위해 업무 필수 컬럼은 DB NOT NULL 대신 서비스 계층에서 draft=false 조건부 검증.
 */
@Entity
@Table(name = "work_requests", indexes = {
        @Index(name = "idx_wr_requester_status", columnList = "requester_id, status"),
        @Index(name = "idx_wr_status_submitted", columnList = "status, submitted_at")
})
@Getter
@Setter
@NoArgsConstructor
public class WorkRequest {

    @Id
    private UUID id;

    /** 업무 번호 WR-YYYYMMDD-NNN. 서버 채번, 화면 표시·검색용 자연키 (ERD 변경 #1) */
    @Column(name = "request_no", unique = true, nullable = false, length = 20)
    private String requestNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(length = 80)
    private String equipment;

    @Column(length = 50)
    private String line;

    @Column(length = 80)
    private String substance;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operating_condition")
    private Map<String, Object> operatingCondition;

    @Column(name = "product_name", length = 120)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 20)
    private ProductType productType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spec_json")
    private Map<String, Object> specJson;

    @Column(columnDefinition = "text")
    private String symptom;

    @Column(name = "site_memo", columnDefinition = "text")
    private String siteMemo;

    @Column(name = "engineer_note", columnDefinition = "text")
    private String engineerNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkRequestStatus status = WorkRequestStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** PENDING 전환 시각. 재제출 시 갱신. 목록 기본 정렬 키 */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    public boolean ownedBy(User user) {
        return requester.getId().equals(user.getId());
    }
}
