package com.skala.argos.repository;

import com.skala.argos.domain.WorkRequest;
import com.skala.argos.domain.WorkRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WorkRequestRepository extends JpaRepository<WorkRequest, UUID> {

    Page<WorkRequest> findByRequesterId(UUID requesterId, Pageable pageable);

    Page<WorkRequest> findByRequesterIdAndStatusIn(UUID requesterId, Collection<WorkRequestStatus> statuses,
                                                   Pageable pageable);

    Page<WorkRequest> findByStatusIn(Collection<WorkRequestStatus> statuses, Pageable pageable);

    /** request_no 채번용: 당일 프리픽스의 마지막 번호 조회 (데모 규모 기준. 동시성은 ERD 메모 #7 참고) */
    Optional<WorkRequest> findTopByRequestNoStartingWithOrderByRequestNoDesc(String prefix);
}
