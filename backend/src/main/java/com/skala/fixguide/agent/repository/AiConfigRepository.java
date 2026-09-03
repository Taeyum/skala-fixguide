package com.skala.fixguide.agent.repository;

import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.agent.entity.AiConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConfigRepository extends JpaRepository<AiConfig, UUID> {

    Optional<AiConfig> findByAgentCodeAndActiveTrue(AgentCode agentCode);
}
