package com.skala.fixguide.workrequest.repository;

import com.skala.fixguide.workrequest.entity.WorkRequestPhoto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRequestPhotoRepository extends JpaRepository<WorkRequestPhoto, UUID> {

    List<WorkRequestPhoto> findByWorkRequestIdOrderByUploadedAt(UUID workRequestId);

    long countByWorkRequestId(UUID workRequestId);
}
