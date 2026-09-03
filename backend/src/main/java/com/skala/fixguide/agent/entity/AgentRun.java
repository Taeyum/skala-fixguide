package com.skala.fixguide.agent.entity;

import com.skala.fixguide.common.entity.BaseTimeEntity;
import com.skala.fixguide.common.entity.JsonMapConverter;
import com.skala.fixguide.workrequest.entity.WorkRequest;
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

/** ERD 4. agent_runs — "AI 검증 시작" 한 번 = run 하나. 재실행하면 새 행 (append-only) */
@Entity
@Getter
@Table(name = "agent_runs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentRun extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RunStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    /** [ERD 추가 제안 #3] 실행 시점의 요청 전체 컨텍스트 스냅샷 */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "input_snapshot", columnDefinition = "text")
    private Map<String, Object> inputSnapshot;

    /** [ERD 추가 제안 #5] 어떤 설정(provider·model)으로 실행됐는지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_config_id")
    private AiConfig aiConfig;

    @Builder
    private AgentRun(
            UUID id,
            WorkRequest workRequest,
            OffsetDateTime startedAt,
            Map<String, Object> inputSnapshot,
            AiConfig aiConfig) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.workRequest = workRequest;
        this.status = RunStatus.RUNNING;
        this.startedAt = startedAt;
        this.inputSnapshot = inputSnapshot;
        this.aiConfig = aiConfig;
    }

    public void finish(OffsetDateTime finishedAt) {
        this.status = RunStatus.DONE;
        this.finishedAt = finishedAt;
    }
}
