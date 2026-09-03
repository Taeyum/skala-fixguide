package com.skala.argos.repository;

import com.skala.argos.domain.WorkRequestPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkRequestPhotoRepository extends JpaRepository<WorkRequestPhoto, UUID> {

    List<WorkRequestPhoto> findByWorkRequestIdOrderByUploadedAt(UUID workRequestId);

    long countByWorkRequestId(UUID workRequestId);
}
