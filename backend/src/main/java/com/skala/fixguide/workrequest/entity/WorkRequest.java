package com.skala.fixguide.workrequest.entity;

import com.skala.fixguide.common.entity.BaseTimeEntity;
import com.skala.fixguide.common.entity.JsonMapConverter;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부품 교체 요청. 상태머신의 주체이며 상태 전이는 이 클래스의 메서드로만 일어난다.
 * DRAFT 를 허용하기 위해 업무 필수 컬럼은 DB NOT NULL 대신 서비스 계층에서 조건부 검증한다.
 */
@Entity
@Getter
@Table(name = "work_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkRequest extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** 업무 번호 WR-YYYYMMDD-NNN. 서버 채번, 화면 표시·검색용 자연키 (ERD 변경 #1) */
    @Column(name = "request_no", unique = true, length = 20)
    private String requestNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "equipment", length = 100)
    private String equipment;

    @Column(name = "line", length = 100)
    private String line;

    @Column(name = "substance", length = 100)
    private String substance;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "operating_condition", columnDefinition = "text")
    private Map<String, Object> operatingCondition;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 30)
    private ProductType productType;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "spec_json", columnDefinition = "text")
    private Map<String, Object> specJson;

    @Column(name = "symptom", columnDefinition = "text")
    private String symptom;

    @Column(name = "site_memo", columnDefinition = "text")
    private String siteMemo;

    @Column(name = "engineer_note", columnDefinition = "text")
    private String engineerNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkRequestStatus status;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Builder
    private WorkRequest(
            UUID id,
            String requestNo,
            User requester,
            String equipment,
            String line,
            String substance,
            Map<String, Object> operatingCondition,
            String productName,
            ProductType productType,
            Map<String, Object> specJson,
            String symptom,
            String siteMemo,
            String engineerNote,
            WorkRequestStatus status,
            OffsetDateTime submittedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.requestNo = requestNo;
        this.requester = requester;
        this.equipment = equipment;
        this.line = line;
        this.substance = substance;
        this.operatingCondition = operatingCondition;
        this.productName = productName;
        this.productType = productType;
        this.specJson = specJson;
        this.symptom = symptom;
        this.siteMemo = siteMemo;
        this.engineerNote = engineerNote;
        this.status = status == null ? WorkRequestStatus.DRAFT : status;
        this.submittedAt = submittedAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return requester != null && requester.getId().equals(userId);
    }

    /** 5.8 PATCH — null 이 아닌 필드만 반영한다 (부분 수정). */
    public void applyPatch(
            String equipment,
            String line,
            String substance,
            Map<String, Object> operatingCondition,
            String productName,
            ProductType productType,
            Map<String, Object> specJson,
            String symptom,
            String siteMemo,
            String engineerNote) {
        if (equipment != null) this.equipment = equipment;
        if (line != null) this.line = line;
        if (substance != null) this.substance = substance;
        if (operatingCondition != null) this.operatingCondition = operatingCondition;
        if (productName != null) this.productName = productName;
        if (productType != null) this.productType = productType;
        if (specJson != null) this.specJson = specJson;
        if (symptom != null) this.symptom = symptom;
        if (siteMemo != null) this.siteMemo = siteMemo;
        if (engineerNote != null) this.engineerNote = engineerNote;
    }

    /** 5.14 제출 — REJECTED 에서 재제출하면 PENDING 으로 복귀하고 submittedAt 을 갱신한다 (AC 6-4). */
    public void submit(String engineerNote, OffsetDateTime submittedAt) {
        this.engineerNote = engineerNote;
        this.status = WorkRequestStatus.PENDING;
        this.submittedAt = submittedAt;
    }

    /** 5.11 AI 검증 시작 */
    public void startAi() {
        this.status = WorkRequestStatus.AI_RUNNING;
    }

    /** 5.12 AI 3종 모두 완료 */
    public void finishAi() {
        this.status = WorkRequestStatus.AI_DONE;
    }

    /** 5.15 승인·거절 결과 반영 */
    public void decide(ApprovalDecision decision) {
        this.status = decision == ApprovalDecision.APPROVE
                ? WorkRequestStatus.APPROVED
                : WorkRequestStatus.REJECTED;
    }
}
