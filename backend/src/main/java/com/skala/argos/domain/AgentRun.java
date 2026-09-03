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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** ERD 4. agent_runs — "AI 검증 시작" 한 번 = run 하나. 재실행하면 새 행 (append-only) */
@Entity
@Table(name = "agent_runs")
@Getter
@Setter
@NoArgsConstructor
public class AgentRun {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    /** [ERD 추가 제안 #3] 실행 시점의 요청 전체 컨텍스트 스냅샷 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot")
    private Map<String, Object> inputSnapshot;

    /** [ERD 추가 제안 #5] 어떤 설정(provider·model)으로 실행됐는지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_config_id")
    private AiConfig aiConfig;
}
