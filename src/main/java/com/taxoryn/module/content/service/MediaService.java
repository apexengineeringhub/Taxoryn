package com.taxoryn.module.content.service;

import com.taxoryn.module.content.dto.MediaAssetDto;
import com.taxoryn.module.content.dto.UpdateMediaAssetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MediaService {

    Page<MediaAssetDto> getMediaAssets(String search, Pageable pageable);

    MediaAssetDto getMediaAsset(UUID id);

    MediaAssetDto uploadMedia(MultipartFile file, String altText, UUID userId, String userName);

    MediaAssetDto updateMedia(UUID id, UpdateMediaAssetRequest request);

    void deleteMedia(UUID id);

    byte[] getMediaContent(UUID id);

    String getMediaContentType(UUID id);
}
