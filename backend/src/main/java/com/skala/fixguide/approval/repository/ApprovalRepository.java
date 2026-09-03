package com.skala.fixguide.approval.repository;

import com.skala.fixguide.approval.entity.Approval;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.dashboard.dto.RejectReasonCount;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    /** append-only 이력 중 최신 1건을 상세 응답 approval 에 노출한다 (ERD 7장) */
    Optional<Approval> findTopByWorkRequestIdOrderByDecidedAtDesc(UUID workRequestId);

    long countByDecidedAtBetween(OffsetDateTime from, OffsetDateTime to);

    long countByDecisionAndDecidedAtBetween(
            ApprovalDecision decision, OffsetDateTime from, OffsetDateTime to);

    /**
     * 안전관리자 대시보드의 "거절 사유 TOP5" (AC 7-3).
     * reasonCategory 가 비어 있는 이력은 집계에서 제외한다.
     */
    @Query("""
            select new com.skala.fixguide.dashboard.dto.RejectReasonCount(a.reasonCategory, count(a))
            from Approval a
            where a.decision = com.skala.fixguide.approval.entity.ApprovalDecision.REJECT
              and a.reasonCategory is not null
              and a.decidedAt >= :from
            group by a.reasonCategory
            order by count(a) desc, a.reasonCategory asc
            """)
    List<RejectReasonCount> findRejectReasonRanking(
            @Param("from") OffsetDateTime from, Pageable pageable);
}
