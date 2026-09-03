package com.skala.fixguide.agent.entity;

import com.skala.fixguide.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ERD 8. ai_configs — AI-Ready "Security & Config Isolation".
 * Mock → 실제 LLM 전환은 provider 값 변경으로 한다. API 키는 저장하지 않는다(환경변수).
 */
@Entity
@Getter
@Table(name = "ai_configs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiConfig extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_code", nullable = false, length = 10)
    private AgentCode agentCode;

    /** MOCK / LOCAL_LLM / OPENAI */
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "model_name", length = 60)
    private String modelName;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @Column(name = "temperature", precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "egress_allowed", nullable = false)
    private boolean egressAllowed;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    private AiConfig(
            UUID id,
            AgentCode agentCode,
            String provider,
            String modelName,
            String promptVersion,
            BigDecimal temperature,
            Integer maxTokens,
            boolean egressAllowed,
            boolean active) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.agentCode = agentCode;
        this.provider = provider;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.egressAllowed = egressAllowed;
        this.active = active;
    }
}
