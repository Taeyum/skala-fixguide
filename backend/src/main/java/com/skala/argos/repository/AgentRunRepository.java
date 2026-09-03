package com.skala.argos.repository;

import com.skala.argos.domain.AgentRun;
import com.skala.argos.domain.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    boolean existsByWorkRequestIdAndStatus(UUID workRequestId, RunStatus status);

    /** 화면은 started_at 최신 run 하나만 본다 (ERD 4장) */
    Optional<AgentRun> findTopByWorkRequestIdOrderByStartedAtDesc(UUID workRequestId);
}
