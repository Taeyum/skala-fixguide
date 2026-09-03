package com.skala.fixguide.workrequest.service;

import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.workrequest.dto.PhotoListResponse;
import com.skala.fixguide.workrequest.dto.PhotoResponse;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestPhoto;
import com.skala.fixguide.workrequest.repository.WorkRequestPhotoRepository;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** API 9·10 — 제품 사진 업로드/열람. 파일은 app.upload-dir 아래에 {workRequestId}/{photoId}.{ext} 로 저장한다. */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkRequestPhotoService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_PHOTOS_PER_REQUEST = 5;

    private final WorkRequestPhotoRepository photoRepository;
    private final WorkRequestRepository workRequestRepository;
    private final WorkRequestAccessPolicy accessPolicy;
    private final Clock clock;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** 5.9 POST /work-requests/{id}/photos — multipart, 요청당 최대 5장 */
    public PhotoListResponse upload(AuthenticatedUser me, UUID workRequestId, List<MultipartFile> files) {
        WorkRequest wr = getWorkRequest(workRequestId);
        accessPolicy.requireOwner(me, wr);

        if (files == null || files.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "files 는 최소 1개 필요합니다.");
        }
        long existing = photoRepository.countByWorkRequestId(workRequestId);
        if (existing + files.size() > MAX_PHOTOS_PER_REQUEST) {
            throw new ApiException(ErrorCode.PHOTO_LIMIT_EXCEEDED);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        List<PhotoResponse> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            String extension = extensionOf(file.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "jpg/png/webp 형식만 업로드할 수 있습니다.");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ApiException(ErrorCode.FILE_TOO_LARGE, "파일당 최대 10MB까지 업로드할 수 있습니다.");
            }

            UUID photoId = UUID.randomUUID();
            String storageKey = workRequestId + "/" + photoId + "." + extension;
            store(file, storageKey);

            WorkRequestPhoto photo = photoRepository.save(WorkRequestPhoto.builder()
                    .id(photoId)
                    .workRequest(wr)
                    .fileName(file.getOriginalFilename())
                    .storageKey(storageKey)
                    .thumbnailKey(storageKey)
                    .size((int) file.getSize())
                    .uploadedAt(now)
                    .build());
            saved.add(PhotoResponse.from(photo));
        }
        return new PhotoListResponse(saved);
    }

    /** 5.10 GET /work-requests/{id}/photos */
    @Transactional(readOnly = true)
    public PhotoListResponse list(AuthenticatedUser me, UUID workRequestId) {
        WorkRequest wr = getWorkRequest(workRequestId);
        accessPolicy.requireReadable(me, wr);
        return new PhotoListResponse(photoRepository.findByWorkRequestIdOrderByUploadedAt(workRequestId).stream()
                .map(PhotoResponse::from)
                .toList());
    }

    private WorkRequest getWorkRequest(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.WORK_REQUEST_NOT_FOUND));
    }

    private void store(MultipartFile file, String storageKey) {
        try {
            Path target = Paths.get(uploadDir).toAbsolutePath().resolve(storageKey);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
