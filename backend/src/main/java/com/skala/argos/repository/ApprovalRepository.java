package com.skala.argos.repository;

import com.skala.argos.domain.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    /** append-only 이력 중 최신 1건을 상세 응답 approval에 노출 (ERD 7장) */
    Optional<Approval> findTopByWorkRequestIdOrderByDecidedAtDesc(UUID workRequestId);
}
