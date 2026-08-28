package com.taxoryn.module.content.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.content.dto.MediaAssetDto;
import com.taxoryn.module.content.dto.UpdateMediaAssetRequest;
import com.taxoryn.module.content.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/content/media")
@RequiredArgsConstructor
@Tag(name = "Content Studio - Media Library", description = "Endpoints for managing Media Library assets")
public class MediaAdminController {

    private final MediaService mediaService;

    @GetMapping
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('MEDIA_VIEW') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Get paged list of media assets")
    public ResponseEntity<ApiResponse<PagedResponse<MediaAssetDto>>> getMediaAssets(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MediaAssetDto> result = mediaService.getMediaAssets(
                search,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('MEDIA_VIEW') or hasAuthority('CONTENT_VIEW')")
    @Operation(summary = "Get single media asset metadata")
    public ResponseEntity<ApiResponse<MediaAssetDto>> getMediaAsset(@PathVariable UUID id) {
        MediaAssetDto asset = mediaService.getMediaAsset(id);
        return ResponseEntity.ok(ApiResponse.success(asset));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('MEDIA_UPLOAD') or hasAuthority('CONTENT_CREATE')")
    @Operation(summary = "Upload image asset to Media Library")
    public ResponseEntity<ApiResponse<MediaAssetDto>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText
    ) {
        UUID userId = SecurityUtils.getCurrentUser().map(com.taxoryn.core.security.SecurityUser::getUserId).orElse(null);
        String userName = SecurityUtils.getCurrentUserEmail();
        MediaAssetDto uploaded = mediaService.uploadMedia(file, altText, userId, userName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Media asset uploaded successfully", uploaded));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('MEDIA_UPLOAD') or hasAuthority('CONTENT_EDIT')")
    @Operation(summary = "Update media asset metadata (alt text)")
    public ResponseEntity<ApiResponse<MediaAssetDto>> updateMedia(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMediaAssetRequest request
    ) {
        MediaAssetDto updated = mediaService.updateMedia(id, request);
        return ResponseEntity.ok(ApiResponse.success("Media asset updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TAXORYN_CONTENT_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('MEDIA_DELETE') or hasAuthority('CONTENT_ARCHIVE')")
    @Operation(summary = "Delete media asset")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable UUID id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok(ApiResponse.success("Media asset deleted successfully", null));
    }
}
