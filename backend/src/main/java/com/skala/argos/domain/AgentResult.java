package com.skala.argos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** ERD 6. agent_results — AI 결과물 (엔지니어 수정 대상). payload_json 전체 치환 방식 */
@Entity
@Table(name = "agent_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_run_agent", columnNames = {"run_id", "agent_code"}))
@Getter
@Setter
@NoArgsConstructor
public class AgentResult {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRun run;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_code", nullable = false, length = 10)
    private AgentCode agentCode;

    /** A1·A2는 items[], A3는 documents[] 구조 (ERD 6장) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false)
    private Map<String, Object> payloadJson;

    /** 항목 중 하나라도 수정·추가·삭제되면 true. S_02 "엔지니어 수정" 배지 */
    @Column(nullable = false)
    private boolean edited = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** [ERD 추가 제안 #4] AI 원본 스냅샷 보존. payload_json은 수정본 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_json")
    private Map<String, Object> originalJson;
}
