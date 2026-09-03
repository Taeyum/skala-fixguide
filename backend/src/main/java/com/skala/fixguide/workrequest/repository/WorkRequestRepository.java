package com.skala.fixguide.workrequest.repository;

import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRequestRepository extends JpaRepository<WorkRequest, UUID> {

    /** 엔지니어 "내 요청" 목록 (mine=true) */
    @EntityGraph(attributePaths = "requester")
    Page<WorkRequest> findByRequesterIdAndStatusIn(
            UUID requesterId, Collection<WorkRequestStatus> statuses, Pageable pageable);

    /** 안전관리자 요청 관리 목록 — 노출 범위는 서비스에서 PENDING 이후로 제한한다. */
    @EntityGraph(attributePaths = "requester")
    Page<WorkRequest> findByStatusIn(Collection<WorkRequestStatus> statuses, Pageable pageable);

    long countByRequesterIdAndStatus(UUID requesterId, WorkRequestStatus status);

    long countByStatus(WorkRequestStatus status);

    /** request_no 채번용: 당일 프리픽스의 마지막 번호 (데모 규모 기준, 동시성은 ERD 메모 #7) */
    Optional<WorkRequest> findTopByRequestNoStartingWithOrderByRequestNoDesc(String prefix);
}
