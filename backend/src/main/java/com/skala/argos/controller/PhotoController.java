package com.skala.argos.controller;

import com.skala.argos.dto.WorkRequestDtos.PhotosResponse;
import com.skala.argos.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** API 9·10 — 제품 사진 업로드/열람 (multipart/form-data) */
@RestController
@RequestMapping("/work-requests/{id}/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhotosResponse upload(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                 @PathVariable UUID id,
                                 @RequestParam("files") List<MultipartFile> files) {
        return photoService.upload(userId, id, files);
    }

    @GetMapping
    public PhotosResponse list(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                               @PathVariable UUID id) {
        return photoService.list(userId, id);
    }
}
