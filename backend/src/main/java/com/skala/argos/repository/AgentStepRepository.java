package com.skala.argos.repository;

import com.skala.argos.domain.AgentStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentStepRepository extends JpaRepository<AgentStep, UUID> {

    List<AgentStep> findByRunIdOrderByAgentCode(UUID runId);
}
