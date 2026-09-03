package com.skala.fixguide.workrequest.controller;

import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.workrequest.dto.PhotoListResponse;
import com.skala.fixguide.workrequest.service.WorkRequestPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "WorkRequest", description = "제품 사진 업로드·열람 (화면 WRA_E_02 · WRA_S_02)")
@RestController
@RequestMapping("/api/v1/work-requests/{id}/photos")
@RequiredArgsConstructor
public class WorkRequestPhotoController {

    private final WorkRequestPhotoService photoService;

    @Operation(summary = "제품 사진 업로드", description = "multipart/form-data, files 파트. jpg/png/webp, 파일당 10MB, 요청당 5장")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoListResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable UUID id,
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(photoService.upload(me, id, files));
    }

    @Operation(summary = "제품 사진 목록")
    @GetMapping
    public ResponseEntity<PhotoListResponse> list(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        return ResponseEntity.ok(photoService.list(me, id));
    }
}
