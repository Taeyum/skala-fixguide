package com.skala.argos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ERD 8. ai_configs [추가 제안] — AI-Ready "Security & Config Isolation".
 * Mock → 실제 LLM 전환은 provider 값 변경으로. API 키는 저장하지 않음(환경변수).
 */
@Entity
@Table(name = "ai_configs")
@Getter
@Setter
@NoArgsConstructor
public class AiConfig {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_code", nullable = false, length = 10)
    private AgentCode agentCode;

    /** MOCK / LOCAL_LLM / OPENAI */
    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "model_name", length = 60)
    private String modelName;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @Column(precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "egress_allowed", nullable = false)
    private boolean egressAllowed = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
