package com.taxoryn.module.document.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.InternalServerException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.DocumentFilterRequest;
import com.taxoryn.module.document.dto.UpdateDocumentRequest;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.StorageProvider;
import com.taxoryn.module.document.mapper.DocumentMapper;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.storage.DocumentStorageService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final ClientRepository clientRepository;
    private final com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;
    private final DocumentMapper documentMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;

    @Override
    @Transactional
    public DocumentDto uploadDocument(MultipartFile file, UploadDocumentRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }

        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        // Check MAX_STORAGE Subscription Limit
        subscriptionService.checkStorageLimit(organizationId, file.getSize());

        if (request.getClientId() != null) {
            clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InternalServerException("Failed to read uploaded file stream: " + e.getMessage());
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document.bin";
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
        String checksum = calculateSha256(bytes);

        // Store file in configured storage backend (Local / S3)
        String storageKey = storageService.store(organizationId, originalFilename, contentType, bytes);
        StorageProvider provider = "S3".equalsIgnoreCase(storageService.getStorageProviderName()) ? StorageProvider.S3 : StorageProvider.LOCAL;

        DocumentEntity entity = DocumentEntity.builder()
                .clientId(request.getClientId())
                .gstFilingId(request.getGstFilingId())
                .itrReturnId(request.getItrReturnId())
                .taskId(request.getTaskId())
                .documentType(request.getDocumentType())
                .fileName(originalFilename)
                .contentType(contentType)
                .fileSize(file.getSize())
                .storageKey(storageKey)
                .storageProvider(provider)
                .financialYear(request.getFinancialYear())
                .assessmentYear(request.getAssessmentYear())
                .status(DocumentStatus.ACTIVE)
                .checksum(checksum)
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        DocumentEntity saved = documentRepository.save(entity);
        log.info("Uploaded document: id={}, name={}, size={} bytes, storageKey={} for tenant={}",
                saved.getId(), saved.getFileName(), saved.getFileSize(), saved.getStorageKey(), organizationId);

        DocumentDto result = enrichDto(saved);
        auditService.logEvent("DOCUMENT_UPLOADED", "DOCUMENT", saved.getId().toString(), null, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownloadDto downloadDocument(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentEntity document = documentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        if (document.getStatus() == DocumentStatus.DELETED) {
            throw new ResourceNotFoundException("Document has been deleted", "id", id);
        }

        byte[] data = storageService.retrieve(document.getStorageKey());
        auditService.logEvent("DOCUMENT_DOWNLOADED", "DOCUMENT", id.toString(), null, document.getFileName());

        return DocumentDownloadDto.builder()
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .data(data)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto getDocumentById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentEntity document = documentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        return enrichDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentDto> getDocuments(DocumentFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<DocumentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }

            if (filterRequest.getDocumentType() != null) {
                predicates.add(cb.equal(root.get("documentType"), filterRequest.getDocumentType()));
            }

            if (StringUtils.hasText(filterRequest.getFinancialYear())) {
                predicates.add(cb.equal(root.get("financialYear"), filterRequest.getFinancialYear().trim()));
            }

            if (StringUtils.hasText(filterRequest.getAssessmentYear())) {
                predicates.add(cb.equal(root.get("assessmentYear"), filterRequest.getAssessmentYear().trim()));
            }

            if (filterRequest.getGstFilingId() != null) {
                predicates.add(cb.equal(root.get("gstFilingId"), filterRequest.getGstFilingId()));
            }

            if (filterRequest.getItrReturnId() != null) {
                predicates.add(cb.equal(root.get("itrReturnId"), filterRequest.getItrReturnId()));
            }

            if (filterRequest.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), filterRequest.getTaskId()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            } else {
                predicates.add(cb.notEqual(root.get("status"), DocumentStatus.DELETED));
            }

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fileName")), pattern),
                        cb.like(cb.lower(root.get("notes")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DocumentEntity> page = documentRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> getClientDocuments(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        List<DocumentEntity> docs = documentRepository.findAllByOrganizationIdAndClientIdAndStatus(
                organizationId, clientId, DocumentStatus.ACTIVE);

        return docs.stream().map(this::enrichDto).toList();
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentEntity document = documentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        // Delete from storage
        storageService.delete(document.getStorageKey());

        // Soft delete metadata
        DocumentStatus oldStatus = document.getStatus();
        document.setStatus(DocumentStatus.DELETED);
        documentRepository.save(document);

        log.info("Deleted document: id={}, key={} for tenant={}", id, document.getStorageKey(), organizationId);
        auditService.logEvent("DOCUMENT_DELETED", "DOCUMENT", id.toString(), oldStatus != null ? oldStatus.name() : null, DocumentStatus.DELETED.name());
    }

    @Override
    @Transactional
    public DocumentDto updateDocumentMetadata(UUID id, UpdateDocumentRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        DocumentEntity document = documentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        DocumentDto oldSnapshot = enrichDto(document);

        if (request.getDocumentType() != null) {
            document.setDocumentType(request.getDocumentType());
        }
        if (request.getFinancialYear() != null) {
            document.setFinancialYear(request.getFinancialYear());
        }
        if (request.getAssessmentYear() != null) {
            document.setAssessmentYear(request.getAssessmentYear());
        }
        if (request.getStatus() != null) {
            document.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            document.setNotes(request.getNotes());
        }

        DocumentEntity saved = documentRepository.save(document);
        log.info("Updated document metadata: id={} for tenant={}", id, organizationId);
        DocumentDto result = enrichDto(saved);
        auditService.logEvent("DOCUMENT_UPDATED", "DOCUMENT", id.toString(), oldSnapshot, result);
        return result;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private DocumentDto enrichDto(DocumentEntity entity) {
        if (entity == null) return null;
        DocumentDto dto = documentMapper.toDto(entity);
        if (dto == null) return null;
        dto.setFileSizeFormatted(formatFileSize(entity.getFileSize()));
        dto.setUploadedBy(entity.getCreatedBy());
        dto.setUploadedAt(entity.getCreatedAt());

        if (entity.getClientId() != null) {
            clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                    .ifPresent(c -> dto.setClientName(c.getDisplayName()));
        }

        return dto;
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
