package com.skala.fixguide.agent.repository;

import com.skala.fixguide.agent.entity.AgentStep;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentStepRepository extends JpaRepository<AgentStep, UUID> {

    List<AgentStep> findByRunIdOrderByAgentCode(UUID runId);
}
