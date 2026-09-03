package com.skala.argos.repository;

import com.skala.argos.domain.AgentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentResultRepository extends JpaRepository<AgentResult, UUID> {

    List<AgentResult> findByRunIdOrderByAgentCode(UUID runId);
}
