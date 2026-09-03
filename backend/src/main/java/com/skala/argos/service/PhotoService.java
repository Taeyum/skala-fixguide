package com.skala.argos.service;

import com.skala.argos.common.ApiException;
import com.skala.argos.common.KstTime;
import com.skala.argos.domain.User;
import com.skala.argos.domain.WorkRequest;
import com.skala.argos.domain.WorkRequestPhoto;
import com.skala.argos.dto.WorkRequestDtos.PhotoView;
import com.skala.argos.dto.WorkRequestDtos.PhotosResponse;
import com.skala.argos.repository.WorkRequestPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 명세 5.9·5.10 제품 사진 업로드·열람. 로컬 디스크 저장.
 * 스캐폴딩 단계 단순화: 썸네일(320px)·EXIF 제거는 미구현, thumbnailUrl은 원본과 동일.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PhotoService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_PHOTOS_PER_REQUEST = 5;

    private final WorkRequestPhotoRepository photoRepository;
    private final WorkRequestService workRequestService;
    private final UserFinder userFinder;
    private final AccessPolicy accessPolicy;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public static String publicUrl(String storageKey) {
        return "/api/v1/files/" + storageKey;
    }

    /** 5.9 POST /work-requests/{id}/photos — multipart, 요청당 최대 5장 */
    public PhotosResponse upload(UUID userId, UUID workRequestId, List<MultipartFile> files) {
        User user = userFinder.get(userId);
        WorkRequest wr = workRequestService.getOrThrow(workRequestId);
        accessPolicy.requireOwner(user, wr);

        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "files는 최소 1개 필요합니다.");
        }
        long existing = photoRepository.countByWorkRequestId(workRequestId);
        if (existing + files.size() > MAX_PHOTOS_PER_REQUEST) {
            throw new ApiException(HttpStatus.CONFLICT, "PHOTO_LIMIT_EXCEEDED",
                    "요청당 사진은 최대 " + MAX_PHOTOS_PER_REQUEST + "장까지 업로드할 수 있습니다.");
        }

        List<PhotoView> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            String extension = extensionOf(file.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE",
                        "jpg/png/webp 형식만 업로드할 수 있습니다.");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE",
                        "파일당 최대 10MB까지 업로드할 수 있습니다.");
            }

            UUID photoId = UUID.randomUUID();
            String storageKey = workRequestId + "/" + photoId + "." + extension;
            store(file, storageKey);

            WorkRequestPhoto photo = new WorkRequestPhoto();
            photo.setId(photoId);
            photo.setWorkRequest(wr);
            photo.setFileName(file.getOriginalFilename());
            photo.setStorageKey(storageKey);
            photo.setThumbnailKey(storageKey);
            photo.setSize((int) file.getSize());
            photo.setUploadedAt(Instant.now());
            photoRepository.save(photo);

            saved.add(toView(photo));
        }
        return new PhotosResponse(saved);
    }

    /** 5.10 GET /work-requests/{id}/photos */
    @Transactional(readOnly = true)
    public PhotosResponse list(UUID userId, UUID workRequestId) {
        User user = userFinder.get(userId);
        WorkRequest wr = workRequestService.getOrThrow(workRequestId);
        accessPolicy.requireReadable(user, wr);
        List<PhotoView> photos = photoRepository.findByWorkRequestIdOrderByUploadedAt(workRequestId).stream()
                .map(this::toView)
                .toList();
        return new PhotosResponse(photos);
    }

    private PhotoView toView(WorkRequestPhoto photo) {
        return new PhotoView(photo.getId(), photo.getFileName(), photo.getSize(),
                publicUrl(photo.getThumbnailKey()), publicUrl(photo.getStorageKey()),
                KstTime.of(photo.getUploadedAt()));
    }

    private void store(MultipartFile file, String storageKey) {
        try {
            Path target = Paths.get(uploadDir).toAbsolutePath().resolve(storageKey);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "파일 저장에 실패했습니다.");
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
