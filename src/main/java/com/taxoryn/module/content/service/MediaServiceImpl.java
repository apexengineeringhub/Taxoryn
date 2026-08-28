package com.taxoryn.module.content.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.module.content.dto.MediaAssetDto;
import com.taxoryn.module.content.dto.UpdateMediaAssetRequest;
import com.taxoryn.module.content.entity.MediaAssetEntity;
import com.taxoryn.module.content.repository.MediaAssetRepository;
import com.taxoryn.module.document.storage.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    private final MediaAssetRepository mediaAssetRepository;
    private final DocumentStorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public Page<MediaAssetDto> getMediaAssets(String search, Pageable pageable) {
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        return mediaAssetRepository.searchMediaAssets(cleanSearch, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAssetDto getMediaAsset(UUID id) {
        return toDto(findOrThrow(id));
    }

    @Override
    @Transactional
    public MediaAssetDto uploadMedia(MultipartFile file, String altText, UUID userId, String userName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("Media upload file is required and cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessValidationException("Media file size exceeds maximum allowed limit of 5MB.");
        }

        String rawContentType = file.getContentType();
        String contentType = rawContentType != null ? rawContentType.toLowerCase().trim() : "application/octet-stream";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessValidationException("Unsupported media format: " + contentType + ". Allowed formats: JPG, PNG, WEBP, GIF, SVG.");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename().trim()
                : "media_" + UUID.randomUUID() + ".png";

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read media upload bytes for {}", originalFilename, e);
            throw new BusinessValidationException("Failed to read uploaded media content.");
        }

        String storageKey = storageService.store(null, originalFilename, contentType, bytes);

        MediaAssetEntity asset = MediaAssetEntity.builder()
                .filename(originalFilename)
                .contentType(contentType)
                .fileSize(file.getSize())
                .storageKey(storageKey)
                .publicUrl("")
                .altText(StringUtils.hasText(altText) ? altText.trim() : originalFilename)
                .uploadedById(userId)
                .uploadedByName(StringUtils.hasText(userName) ? userName : "Taxoryn Admin")
                .build();

        MediaAssetEntity saved = mediaAssetRepository.save(asset);
        saved.setPublicUrl("/api/v1/public/media/" + saved.getId());
        saved = mediaAssetRepository.save(saved);
        log.info("Uploaded platform media asset: id={}, filename='{}', size={} bytes", saved.getId(), saved.getFilename(), saved.getFileSize());

        return toDto(saved);
    }

    @Override
    @Transactional
    public MediaAssetDto updateMedia(UUID id, UpdateMediaAssetRequest request) {
        MediaAssetEntity asset = findOrThrow(id);
        if (request != null && request.getAltText() != null) {
            asset.setAltText(request.getAltText().trim());
        }
        MediaAssetEntity updated = mediaAssetRepository.save(asset);
        log.info("Updated media asset metadata: id={}", updated.getId());
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID id) {
        MediaAssetEntity asset = findOrThrow(id);
        try {
            storageService.delete(asset.getStorageKey());
        } catch (Exception e) {
            log.warn("Failed to delete storage file key {} for media id {}", asset.getStorageKey(), id, e);
        }
        mediaAssetRepository.delete(asset);
        log.info("Deleted media asset: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getMediaContent(UUID id) {
        MediaAssetEntity asset = findOrThrow(id);
        return storageService.retrieve(asset.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public String getMediaContentType(UUID id) {
        MediaAssetEntity asset = findOrThrow(id);
        return asset.getContentType();
    }

    private MediaAssetEntity findOrThrow(UUID id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found with id: " + id));
    }

    private MediaAssetDto toDto(MediaAssetEntity entity) {
        if (entity == null) return null;
        return MediaAssetDto.builder()
                .id(entity.getId())
                .filename(entity.getFilename())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .publicUrl(entity.getPublicUrl())
                .altText(entity.getAltText())
                .uploadedByName(entity.getUploadedByName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
