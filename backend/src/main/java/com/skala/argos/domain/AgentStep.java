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

import java.time.Instant;
import java.util.UUID;

/** ERD 5. agent_steps — E_03 진행 카드 하나 = 행 하나. 폴링 응답 steps[]의 소스 */
@Entity
@Table(name = "agent_steps",
        uniqueConstraints = @UniqueConstraint(name = "uk_step_run_agent", columnNames = {"run_id", "agent_code"}))
@Getter
@Setter
@NoArgsConstructor
public class AgentStep {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRun run;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_code", nullable = false, length = 10)
    private AgentCode agentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStepStatus status = AgentStepStatus.WAITING;

    @Column(length = 200)
    private String message;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
