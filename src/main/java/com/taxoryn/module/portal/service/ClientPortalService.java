package com.taxoryn.module.portal.service;

import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.portal.dto.ClientDocumentRequestDto;
import com.taxoryn.module.portal.dto.ClientGstStatusDto;
import com.taxoryn.module.portal.dto.ClientItrStatusDto;
import com.taxoryn.module.portal.dto.ClientNotificationDto;
import com.taxoryn.module.portal.dto.ClientPortalDashboardDto;
import com.taxoryn.module.portal.dto.ClientPortalProfileDto;
import com.taxoryn.module.portal.dto.ClientPortalUserDto;
import com.taxoryn.module.portal.dto.ClientTaskDto;
import com.taxoryn.module.portal.dto.CreateClientDocumentRequest;
import com.taxoryn.module.portal.dto.RegisterClientPortalUserRequest;
import com.taxoryn.module.portal.dto.UpdateClientPortalProfileRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ClientPortalService {

    ClientPortalUserDto registerClientPortalUser(RegisterClientPortalUserRequest request);

    ClientPortalDashboardDto getDashboard();

    ClientPortalDashboardDto getDashboardForClient(UUID clientId);

    ClientPortalProfileDto getProfile();

    ClientPortalProfileDto updateProfile(UpdateClientPortalProfileRequest request);

    List<ClientGstStatusDto> getGstStatus();

    List<ClientItrStatusDto> getItrStatus();

    List<DocumentDto> getClientDocuments();

    List<ClientDocumentRequestDto> getPendingDocuments();

    DocumentDto uploadClientDocument(MultipartFile file, UploadDocumentRequest request, UUID documentRequestId);

    DocumentDownloadDto downloadClientDocument(UUID documentId);

    DocumentDownloadDto previewClientDocument(UUID documentId);

    List<ClientTaskDto> getClientTasks();

    List<ClientNotificationDto> getClientNotifications();

    void markNotificationRead(UUID notificationId);

    ClientDocumentRequestDto requestDocumentFromClient(CreateClientDocumentRequest request);

    List<com.taxoryn.module.billing.dto.InvoiceDto> getClientInvoices();

    com.taxoryn.module.billing.dto.InvoiceDto getClientInvoiceById(UUID invoiceId);

    List<ClientPortalUserDto> getClientPortalUsers(UUID clientId);
}
