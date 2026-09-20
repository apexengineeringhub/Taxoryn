package com.taxoryn.module.docrequest.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestFilterRequest;
import com.taxoryn.module.docrequest.dto.DocumentRequestSummaryDto;
import com.taxoryn.module.docrequest.dto.RejectDocumentItemRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentRequestService {

    DocumentRequestDto createAndSendRequest(CreateDocumentRequest request);

    DocumentRequestDto getRequestById(UUID id);

    PagedResponse<DocumentRequestDto> getRequests(DocumentRequestFilterRequest filter);

    List<DocumentRequestDto> getClientRequests(UUID clientId);

    DocumentRequestSummaryDto getSummaryStats();

    DocumentRequestDto acceptItem(UUID itemId);

    DocumentRequestDto rejectItem(UUID itemId, RejectDocumentItemRequest request);

    void sendReminder(UUID requestId);

    DocumentRequestDto cancelRequest(UUID requestId);

    DocumentRequestDto uploadItemDocument(UUID itemId, MultipartFile file);

    // Bidirectional Document Exchange & Delivery
    DocumentRequestDto createClientAcknowledgementRequest(com.taxoryn.module.docrequest.dto.CreateClientAcknowledgementRequest request);

    DocumentRequestDto sendDocumentToClient(com.taxoryn.module.docrequest.dto.SendDocumentToClientRequest request, MultipartFile file);

    DocumentRequestDto declineClientRequest(UUID requestId, com.taxoryn.module.docrequest.dto.DeclineDocumentRequest request);

    // Client Portal Access
    List<DocumentRequestDto> getClientPortalRequests();

    List<DocumentRequestDto> getClientPortalDeliveredDocuments();

    DocumentRequestDto getClientPortalRequestById(UUID requestId);

    DocumentRequestDto uploadClientPortalItemDocument(UUID itemId, MultipartFile file);

    void recordClientDocumentViewed(UUID requestId);

    void recordClientDocumentDownloaded(UUID requestId);
}