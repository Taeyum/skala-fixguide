package com.skala.fixguide.workrequest.entity;

import com.skala.fixguide.common.entity.BaseTimeEntity;
import com.skala.fixguide.common.entity.JsonMapConverter;
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
 * 부품 교체 요청. 이번 브랜치(로그인·메인화면)에서는 <b>조회만</b> 사용한다.
 * 등록·수정·제출 로직은 요청 등록/결과 화면 담당자가 이어서 붙인다.
 */
@Entity
@Getter
@Table(name = "work_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkRequest extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

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
}
