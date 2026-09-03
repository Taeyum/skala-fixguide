package com.skala.fixguide.agent.repository;

import com.skala.fixguide.agent.entity.AgentResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentResultRepository extends JpaRepository<AgentResult, UUID> {

    List<AgentResult> findByRunIdOrderByAgentCode(UUID runId);
}
