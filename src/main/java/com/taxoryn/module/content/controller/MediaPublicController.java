package com.taxoryn.module.content.controller;

import com.taxoryn.module.content.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/media")
@RequiredArgsConstructor
@Tag(name = "Public Media", description = "Public endpoints for streaming learning media assets")
public class MediaPublicController {

    private final MediaService mediaService;

    @GetMapping("/{id}")
    @Operation(summary = "Stream media asset content by ID")
    public ResponseEntity<byte[]> streamMedia(@PathVariable UUID id) {
        byte[] content = mediaService.getMediaContent(id);
        String contentType = mediaService.getMediaContentType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/png"))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(content);
    }
}
