package com.skala.fixguide.agent.entity;

import com.skala.fixguide.common.entity.BaseTimeEntity;
import com.skala.fixguide.common.entity.JsonMapConverter;
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
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** ERD 6. agent_results — AI 결과물 (엔지니어 수정 대상). payload_json 전체 치환 방식 */
@Entity
@Getter
@Table(name = "agent_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_run_agent", columnNames = {"run_id", "agent_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentResult extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRun run;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_code", nullable = false, length = 10)
    private AgentCode agentCode;

    /** A1·A2 는 items[], A3 는 documents[] 구조 (ERD 6장) */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private Map<String, Object> payloadJson;

    /** 항목 중 하나라도 수정·추가·삭제되면 true. S_02 "엔지니어 수정" 배지 */
    @Column(name = "edited", nullable = false)
    private boolean edited;

    /** [ERD 추가 제안 #4] AI 원본 스냅샷 보존. payload_json 은 수정본 */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "original_json", columnDefinition = "text")
    private Map<String, Object> originalJson;

    @Builder
    private AgentResult(UUID id, AgentRun run, AgentCode agentCode, Map<String, Object> payloadJson) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.run = run;
        this.agentCode = agentCode;
        this.payloadJson = payloadJson;
        this.originalJson = payloadJson;
        this.edited = false;
    }

    public void replacePayload(Map<String, Object> payloadJson, boolean changed) {
        this.payloadJson = payloadJson;
        this.edited = this.edited || changed;
    }
}
