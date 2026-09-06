package com.taxoryn.module.document.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.DocumentFilterRequest;
import com.taxoryn.module.document.dto.PresignedUrlResponse;
import com.taxoryn.module.document.dto.UpdateDocumentRequest;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentDto uploadDocument(MultipartFile file, UploadDocumentRequest request);

    DocumentDownloadDto downloadDocument(UUID id);

    DocumentDownloadDto previewDocument(UUID id);

    PresignedUrlResponse getDocumentDownloadUrl(UUID id);

    DocumentDto getDocumentById(UUID id);

    PagedResponse<DocumentDto> getDocuments(DocumentFilterRequest filterRequest);

    List<DocumentDto> getClientDocuments(UUID clientId);

    void deleteDocument(UUID id);

    DocumentDto updateDocumentMetadata(UUID id, UpdateDocumentRequest request);
}
