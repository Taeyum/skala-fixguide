package com.skala.fixguide.agent.repository;

import com.skala.fixguide.agent.entity.AgentRun;
import com.skala.fixguide.agent.entity.RunStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    boolean existsByWorkRequestIdAndStatus(UUID workRequestId, RunStatus status);

    /** 화면은 started_at 최신 run 하나만 본다 (ERD 4장) */
    Optional<AgentRun> findTopByWorkRequestIdOrderByStartedAtDesc(UUID workRequestId);
}
