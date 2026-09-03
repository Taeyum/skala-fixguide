package com.skala.argos.repository;

import com.skala.argos.domain.AgentCode;
import com.skala.argos.domain.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiConfigRepository extends JpaRepository<AiConfig, UUID> {

    Optional<AiConfig> findByAgentCodeAndActiveTrue(AgentCode agentCode);
}
