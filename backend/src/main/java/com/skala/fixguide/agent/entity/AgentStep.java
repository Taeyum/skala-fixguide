package com.skala.fixguide.agent.entity;

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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** ERD 5. agent_steps — E_03 진행 카드 하나 = 행 하나. 폴링 응답 steps[] 의 소스 */
@Entity
@Getter
@Table(name = "agent_steps",
        uniqueConstraints = @UniqueConstraint(name = "uk_step_run_agent", columnNames = {"run_id", "agent_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentStep {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRun run;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_code", nullable = false, length = 10)
    private AgentCode agentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentStepStatus status;

    @Column(name = "message", length = 200)
    private String message;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Builder
    private AgentStep(UUID id, AgentRun run, AgentCode agentCode) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.run = run;
        this.agentCode = agentCode;
        this.status = AgentStepStatus.WAITING;
    }

    public void start(OffsetDateTime now, String message) {
        this.status = AgentStepStatus.RUNNING;
        this.startedAt = now;
        this.message = message;
    }

    public void done(OffsetDateTime now, String message) {
        if (this.startedAt == null) {
            this.startedAt = now;
        }
        this.status = AgentStepStatus.DONE;
        this.finishedAt = now;
        this.message = message;
    }

    public boolean isDone() {
        return status == AgentStepStatus.DONE;
    }
}
