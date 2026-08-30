package com.taxoryn.module.billing.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.billing.dto.BillingDashboardStatsDto;
import com.taxoryn.module.billing.dto.ClientBillingHistoryDto;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.dto.InvoiceFilterRequest;
import com.taxoryn.module.billing.dto.InvoicePaymentDto;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.dto.UpdateInvoiceRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import com.taxoryn.module.billing.mapper.InvoiceMapper;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.portal.entity.ClientNotificationEntity;
import com.taxoryn.module.portal.entity.ClientNotificationEntity.NotificationType;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final ClientRepository clientRepository;
    private final ClientNotificationRepository notificationRepository;
    private final InvoiceMapper invoiceMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final com.taxoryn.module.organization.repository.OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public InvoiceDto createInvoice(CreateInvoiceRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        String invoiceNumber = request.getInvoiceNumber();
        if (StringUtils.hasText(invoiceNumber)) {
            invoiceNumber = invoiceNumber.trim().toUpperCase();
            if (invoiceRepository.existsByOrganizationIdAndInvoiceNumber(organizationId, invoiceNumber)) {
                throw new DuplicateResourceException("Invoice", "invoiceNumber", invoiceNumber);
            }
        } else {
            invoiceNumber = generateInvoiceNumber(organizationId);
        }

        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(client.getId())
                .invoiceNumber(invoiceNumber)
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .status(InvoiceStatus.DRAFT)
                .notes(request.getNotes())
                .terms(request.getTerms())
                .build();
        invoice.setOrganizationId(organizationId);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateInvoiceItemRequest itemReq : request.getItems()) {
            BigDecimal qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
            BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal taxRate = itemReq.getTaxRate() != null ? itemReq.getTaxRate() : new BigDecimal("18.00");

            BigDecimal lineSubtotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal lineAmount = lineSubtotal.add(lineTax);

            subtotal = subtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);

            InvoiceItemEntity item = InvoiceItemEntity.builder()
                    .service(itemReq.getService())
                    .description(itemReq.getDescription())
                    .quantity(qty)
                    .unitPrice(price)
                    .taxRate(taxRate)
                    .tax(lineTax)
                    .amount(lineAmount)
                    .build();

            invoice.addItem(item);
        }

        BigDecimal total = subtotal.add(totalTax);
        invoice.setSubtotal(subtotal);
        invoice.setTax(totalTax);
        invoice.setTotal(total);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalanceDue(total);

        InvoiceEntity saved = invoiceRepository.save(invoice);
        log.info("Created invoice: id={}, number={}, total={} for client={} in tenant={}",
                saved.getId(), saved.getInvoiceNumber(), saved.getTotal(), client.getId(), organizationId);

        InvoiceDto result = enrichDto(saved);
        auditService.logEvent("INVOICE_CREATED", "INVOICE", saved.getId().toString(), null, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        InvoiceEntity invoice = invoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        return enrichDto(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InvoiceDto> getInvoices(InvoiceFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<InvoiceEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }

            if (filterRequest.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), filterRequest.getStartDate()));
            }

            if (filterRequest.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), filterRequest.getEndDate()));
            }

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("invoiceNumber")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InvoiceEntity> page = invoiceRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichDto);
    }

    @Override
    @Transactional
    public InvoiceDto updateInvoice(UUID id, UpdateInvoiceRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        InvoiceEntity invoice = invoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        InvoiceDto oldSnapshot = enrichDto(invoice);

        if (invoice.getStatus() != InvoiceStatus.DRAFT && request.getItems() != null && !request.getItems().isEmpty()) {
            throw new BadRequestException("Line items can only be modified when invoice is in DRAFT status");
        }

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }
        if (request.getTerms() != null) {
            invoice.setTerms(request.getTerms());
        }

        if (invoice.getStatus() == InvoiceStatus.DRAFT && request.getItems() != null && !request.getItems().isEmpty()) {
            invoice.getItems().clear();

            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal totalTax = BigDecimal.ZERO;

            for (CreateInvoiceItemRequest itemReq : request.getItems()) {
                BigDecimal qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
                BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal taxRate = itemReq.getTaxRate() != null ? itemReq.getTaxRate() : new BigDecimal("18.00");

                BigDecimal lineSubtotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal lineAmount = lineSubtotal.add(lineTax);

                subtotal = subtotal.add(lineSubtotal);
                totalTax = totalTax.add(lineTax);

                InvoiceItemEntity item = InvoiceItemEntity.builder()
                        .service(itemReq.getService())
                        .description(itemReq.getDescription())
                        .quantity(qty)
                        .unitPrice(price)
                        .taxRate(taxRate)
                        .tax(lineTax)
                        .amount(lineAmount)
                        .build();

                invoice.addItem(item);
            }

            BigDecimal total = subtotal.add(totalTax);
            invoice.setSubtotal(subtotal);
            invoice.setTax(totalTax);
            invoice.setTotal(total);
            invoice.setBalanceDue(total.subtract(invoice.getPaidAmount()));
        }

        InvoiceEntity saved = invoiceRepository.save(invoice);
        log.info("Updated invoice: id={} for tenant={}", saved.getId(), organizationId);
        InvoiceDto result = enrichDto(saved);
        auditService.logEvent("INVOICE_UPDATED", "INVOICE", saved.getId().toString(), oldSnapshot, result);
        return result;
    }

    @Override
    @Transactional
    public InvoiceDto issueInvoice(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        InvoiceEntity invoice = invoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT invoices can be issued. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.ISSUED);
        InvoiceEntity saved = invoiceRepository.save(invoice);

        // Notify client in Client Portal
        ClientNotificationEntity notification = ClientNotificationEntity.builder()
                .clientId(invoice.getClientId())
                .title("Invoice Issued: " + invoice.getInvoiceNumber())
                .message("An invoice for ₹" + invoice.getTotal() + " has been issued. Due date: " + invoice.getDueDate())
                .notificationType(NotificationType.GENERAL)
                .read(false)
                .build();
        notification.setOrganizationId(organizationId);
        notificationRepository.save(notification);

        log.info("Issued invoice: id={}, number={} for tenant={}", saved.getId(), saved.getInvoiceNumber(), organizationId);
        InvoiceDto result = enrichDto(saved);
        auditService.logEvent("INVOICE_STATUS_UPDATED", "INVOICE", saved.getId().toString(), "DRAFT", "ISSUED");

        // Publish InvoiceIssuedEvent for WhatsApp & multi-channel notification
        clientRepository.findByIdAndOrganizationId(invoice.getClientId(), organizationId).ifPresent(client -> {
            String orgName = organizationRepository.findById(organizationId)
                    .map(com.taxoryn.module.organization.entity.OrganizationEntity::getName)
                    .orElse("Tax Practice");

            eventPublisher.publishEvent(com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent.builder()
                    .organizationId(organizationId)
                    .invoiceId(saved.getId())
                    .clientId(client.getId())
                    .clientName(client.getDisplayName())
                    .clientPhone(client.getPhone())
                    .clientEmail(client.getEmail())
                    .organizationName(orgName)
                    .invoiceNumber(saved.getInvoiceNumber())
                    .totalAmount(saved.getTotal())
                    .balanceAmount(saved.getBalanceDue())
                    .currency("INR")
                    .issueDate(saved.getInvoiceDate())
                    .dueDate(saved.getDueDate())
                    .build());
        });

        return result;
    }

    @Override
    @Transactional
    public InvoiceDto cancelInvoice(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        InvoiceEntity invoice = invoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Fully paid invoices cannot be cancelled");
        }

        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(InvoiceStatus.CANCELLED);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        log.info("Cancelled invoice: id={} for tenant={}", saved.getId(), organizationId);
        InvoiceDto result = enrichDto(saved);
        auditService.logEvent("INVOICE_STATUS_UPDATED", "INVOICE", saved.getId().toString(), oldStatus.name(), "CANCELLED");
        return result;
    }

    @Override
    @Transactional
    public InvoicePaymentDto recordPayment(UUID invoiceId, RecordPaymentRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        InvoiceEntity invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Payments cannot be recorded for invoices in " + invoice.getStatus() + " status");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        InvoicePaymentEntity payment = InvoicePaymentEntity.builder()
                .invoice(invoice)
                .clientId(invoice.getClientId())
                .paymentDate(request.getPaymentDate())
                .amount(request.getAmount().setScale(2, RoundingMode.HALF_UP))
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();
        payment.setOrganizationId(organizationId);

        InvoicePaymentEntity savedPayment = invoicePaymentRepository.save(payment);

        // Update Invoice balances and status
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(payment.getAmount());
        BigDecimal newBalanceDue = invoice.getTotal().subtract(newPaidAmount);

        invoice.setPaidAmount(newPaidAmount);
        invoice.setBalanceDue(newBalanceDue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newBalanceDue);

        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);

        // Notify client
        ClientNotificationEntity notification = ClientNotificationEntity.builder()
                .clientId(invoice.getClientId())
                .title("Payment Received: ₹" + payment.getAmount())
                .message("Payment receipt recorded for invoice " + invoice.getInvoiceNumber() +
                        ". Remaining balance: ₹" + invoice.getBalanceDue())
                .notificationType(NotificationType.GENERAL)
                .read(false)
                .build();
        notification.setOrganizationId(organizationId);
        notificationRepository.save(notification);

        log.info("Recorded payment: id={}, amount={}, invoice={}, newStatus={} for tenant={}",
                savedPayment.getId(), savedPayment.getAmount(), invoice.getInvoiceNumber(), invoice.getStatus(), organizationId);

        InvoicePaymentDto paymentDto = invoiceMapper.toPaymentDto(savedPayment);
        auditService.logEvent("INVOICE_PAYMENT_RECORDED", "INVOICE", invoiceId.toString(), null, paymentDto);

        // Publish PaymentReceivedEvent for WhatsApp confirmation
        clientRepository.findByIdAndOrganizationId(invoice.getClientId(), organizationId).ifPresent(client -> {
            String orgName = organizationRepository.findById(organizationId)
                    .map(com.taxoryn.module.organization.entity.OrganizationEntity::getName)
                    .orElse("Tax Practice");

            eventPublisher.publishEvent(com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent.builder()
                    .organizationId(organizationId)
                    .invoiceId(invoice.getId())
                    .paymentId(savedPayment.getId())
                    .clientId(client.getId())
                    .clientName(client.getDisplayName())
                    .clientPhone(client.getPhone())
                    .clientEmail(client.getEmail())
                    .organizationName(orgName)
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .paymentReference(savedPayment.getReferenceNumber())
                    .amountPaid(savedPayment.getAmount())
                    .remainingBalance(invoice.getBalanceDue())
                    .currency("INR")
                    .paymentDate(savedPayment.getPaymentDate())
                    .paymentMethod(savedPayment.getPaymentMethod() != null ? savedPayment.getPaymentMethod().name() : "OTHER")
                    .build());
        });

        return paymentDto;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendInvoiceReminder(UUID invoiceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        InvoiceEntity invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Reminders can only be sent for pending, issued, or overdue invoices");
        }

        clientRepository.findByIdAndOrganizationId(invoice.getClientId(), organizationId).ifPresent(client -> {
            String orgName = organizationRepository.findById(organizationId)
                    .map(com.taxoryn.module.organization.entity.OrganizationEntity::getName)
                    .orElse("Tax Practice");

            int overdueDays = 0;
            if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now())) {
                overdueDays = (int) java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
            }

            eventPublisher.publishEvent(com.taxoryn.module.notification.whatsapp.event.InvoiceReminderEvent.builder()
                    .organizationId(organizationId)
                    .invoiceId(invoice.getId())
                    .clientId(client.getId())
                    .clientName(client.getDisplayName())
                    .clientEmail(client.getEmail())
                    .clientPhone(client.getPhone())
                    .organizationName(orgName)
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .totalAmount(invoice.getTotal())
                    .balanceAmount(invoice.getBalanceDue())
                    .currency("INR")
                    .dueDate(invoice.getDueDate())
                    .overdueDays(overdueDays)
                    .build());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoicePaymentDto> getInvoicePayments(UUID invoiceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        invoiceRepository.findByIdAndOrganizationId(invoiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        return invoiceMapper.toPaymentDtoList(
                invoicePaymentRepository.findAllByOrganizationIdAndInvoiceIdOrderByPaymentDateDesc(organizationId, invoiceId));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientBillingHistoryDto getClientBillingHistory(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        List<InvoiceEntity> invoices = invoiceRepository.findAllByOrganizationIdAndClientIdOrderByInvoiceDateDesc(organizationId, clientId);
        List<InvoicePaymentEntity> payments = invoicePaymentRepository.findAllByOrganizationIdAndClientIdOrderByPaymentDateDesc(organizationId, clientId);

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        long overdueCount = 0;
        long paidCount = 0;

        LocalDate today = LocalDate.now();

        for (InvoiceEntity inv : invoices) {
            if (inv.getStatus() != InvoiceStatus.CANCELLED) {
                totalBilled = totalBilled.add(inv.getTotal());
                totalPaid = totalPaid.add(inv.getPaidAmount());
                totalOutstanding = totalOutstanding.add(inv.getBalanceDue());

                if (inv.getStatus() == InvoiceStatus.PAID) {
                    paidCount++;
                } else if ((inv.getStatus() == InvoiceStatus.ISSUED || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID) && inv.getDueDate().isBefore(today)) {
                    overdueCount++;
                }
            }
        }

        return ClientBillingHistoryDto.builder()
                .clientId(client.getId())
                .clientName(client.getDisplayName())
                .clientGstin(client.getGstin())
                .clientPan(client.getPan())
                .totalBilled(totalBilled)
                .totalPaid(totalPaid)
                .totalOutstanding(totalOutstanding)
                .totalInvoicesCount(invoices.size())
                .overdueInvoicesCount(overdueCount)
                .paidInvoicesCount(paidCount)
                .invoices(invoices.stream().map(this::enrichDto).toList())
                .recentPayments(invoiceMapper.toPaymentDtoList(payments))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDashboardStatsDto getBillingDashboardStats() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<InvoiceEntity> invoices = invoiceRepository.findAllByOrganizationId(organizationId);

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalDraft = BigDecimal.ZERO;

        long draftCount = 0;
        long issuedCount = 0;
        long partiallyPaidCount = 0;
        long paidCount = 0;
        long overdueCount = 0;
        long cancelledCount = 0;

        Map<String, BigDecimal> revenueByService = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (InvoiceEntity inv : invoices) {
            if (inv.getStatus() == InvoiceStatus.DRAFT) {
                draftCount++;
                totalDraft = totalDraft.add(inv.getTotal());
            } else if (inv.getStatus() == InvoiceStatus.CANCELLED) {
                cancelledCount++;
            } else {
                totalBilled = totalBilled.add(inv.getTotal());
                totalCollected = totalCollected.add(inv.getPaidAmount());
                totalOutstanding = totalOutstanding.add(inv.getBalanceDue());

                if (inv.getStatus() == InvoiceStatus.ISSUED) {
                    issuedCount++;
                    if (inv.getDueDate().isBefore(today)) {
                        overdueCount++;
                    }
                } else if (inv.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
                    partiallyPaidCount++;
                    if (inv.getDueDate().isBefore(today)) {
                        overdueCount++;
                    }
                } else if (inv.getStatus() == InvoiceStatus.PAID) {
                    paidCount++;
                }

                // Breakdown by service
                for (InvoiceItemEntity item : inv.getItems()) {
                    String serviceKey = item.getService().name();
                    revenueByService.merge(serviceKey, item.getAmount(), BigDecimal::add);
                }
            }
        }

        return BillingDashboardStatsDto.builder()
                .totalBilled(totalBilled)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .totalDraft(totalDraft)
                .totalInvoices(invoices.size())
                .draftInvoices(draftCount)
                .issuedInvoices(issuedCount)
                .partiallyPaidInvoices(partiallyPaidCount)
                .paidInvoices(paidCount)
                .overdueInvoices(overdueCount)
                .cancelledInvoices(cancelledCount)
                .revenueByService(revenueByService)
                .build();
    }

    @Override
    @Transactional
    public com.taxoryn.module.billing.dto.BulkInvoiceResultDto bulkCreateInvoices(com.taxoryn.module.billing.dto.BulkCreateInvoicesRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        com.taxoryn.module.billing.dto.BulkInvoiceResultDto result = com.taxoryn.module.billing.dto.BulkInvoiceResultDto.builder()
                .createdInvoices(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();

        List<ClientEntity> targetClients;
        if (request.getClientIds() != null && !request.getClientIds().isEmpty()) {
            targetClients = clientRepository.findAllById(request.getClientIds()).stream()
                    .filter(c -> c.getOrganizationId().equals(organizationId) && c.getStatus() == ClientEntity.ClientStatus.ACTIVE)
                    .toList();
        } else {
            targetClients = clientRepository.findAllByOrganizationId(organizationId).stream()
                    .filter(c -> c.getStatus() == ClientEntity.ClientStatus.ACTIVE)
                    .toList();
        }

        result.setTotalProcessed(targetClients.size());
        BigDecimal totalBilled = BigDecimal.ZERO;

        for (ClientEntity client : targetClients) {
            try {
                String invoiceNumber = generateInvoiceNumber(organizationId);
                InvoiceEntity invoice = InvoiceEntity.builder()
                        .clientId(client.getId())
                        .invoiceNumber(invoiceNumber)
                        .invoiceDate(request.getInvoiceDate())
                        .dueDate(request.getDueDate())
                        .status(request.isAutoIssue() ? InvoiceStatus.ISSUED : InvoiceStatus.DRAFT)
                        .notes(request.getNotes())
                        .terms(request.getTerms())
                        .build();
                invoice.setOrganizationId(organizationId);

                BigDecimal subtotal = BigDecimal.ZERO;
                BigDecimal totalTax = BigDecimal.ZERO;
                List<InvoiceItemEntity> items = new ArrayList<>();

                for (CreateInvoiceItemRequest itemReq : request.getItems()) {
                    BigDecimal qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
                    BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal taxRate = itemReq.getTaxRate() != null ? itemReq.getTaxRate() : new BigDecimal("18.00");

                    BigDecimal lineSubtotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal lineAmount = lineSubtotal.add(lineTax);

                    subtotal = subtotal.add(lineSubtotal);
                    totalTax = totalTax.add(lineTax);

                    InvoiceItemEntity item = InvoiceItemEntity.builder()
                            .invoice(invoice)
                            .service(itemReq.getService())
                            .description(itemReq.getDescription())
                            .quantity(qty)
                            .unitPrice(price)
                            .taxRate(taxRate)
                            .tax(lineTax)
                            .amount(lineAmount)
                            .build();
                    items.add(item);
                }

                BigDecimal grandTotal = subtotal.add(totalTax);
                invoice.setSubtotal(subtotal);
                invoice.setTax(totalTax);
                invoice.setTotal(grandTotal);
                invoice.setPaidAmount(BigDecimal.ZERO);
                invoice.setBalanceDue(grandTotal);
                invoice.setItems(items);

                InvoiceEntity saved = invoiceRepository.save(invoice);
                InvoiceDto dto = enrichDto(saved);
                result.getCreatedInvoices().add(dto);
                result.setTotalCreated(result.getTotalCreated() + 1);
                totalBilled = totalBilled.add(grandTotal);

                auditService.logEvent("BULK_INVOICE_GENERATED", "INVOICE", saved.getId().toString(), null, invoiceNumber);
            } catch (Exception ex) {
                result.getErrors().add("Client " + client.getDisplayName() + ": " + ex.getMessage());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        result.setTotalBilledAmount(totalBilled);
        return result;
    }

    @Override
    @Transactional
    public List<InvoiceDto> seedDemoInvoices() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<ClientEntity> clients = clientRepository.findAllByOrganizationId(organizationId);
        List<InvoiceDto> seeded = new ArrayList<>();
        if (clients.isEmpty()) return seeded;

        LocalDate today = LocalDate.now();
        LocalDate invoiceDate = today.minusDays(5);
        LocalDate dueDate = today.plusDays(10);

        for (int i = 0; i < Math.min(clients.size(), 4); i++) {
            ClientEntity client = clients.get(i);
            String invNum = generateInvoiceNumber(organizationId);

            InvoiceEntity.InvoiceStatus status = (i % 2 == 0) ? InvoiceStatus.PAID : InvoiceStatus.ISSUED;
            BigDecimal fee = (i == 0) ? new BigDecimal("7500.00") : (i == 1) ? new BigDecimal("4500.00") : (i == 2) ? new BigDecimal("2500.00") : new BigDecimal("15000.00");
            InvoiceItemEntity.BillingServiceType svc = (i == 0) ? InvoiceItemEntity.BillingServiceType.GST_FILING : (i == 1) ? InvoiceItemEntity.BillingServiceType.ITR_FILING : (i == 2) ? InvoiceItemEntity.BillingServiceType.ACCOUNTING : InvoiceItemEntity.BillingServiceType.AUDIT;
            String desc = (i == 0) ? "Monthly GST Filing (GSTR-1 & 3B) & Reconciliation" : (i == 1) ? "Annual Income Tax Return Computation & CPC Filing" : (i == 2) ? "Monthly Bookkeeping & Financial Statement Preparation" : "Statutory Tax Audit & Form 3CD Certification";

            BigDecimal taxRate = new BigDecimal("18.00");
            BigDecimal lineTax = fee.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal total = fee.add(lineTax);

            InvoiceEntity inv = InvoiceEntity.builder()
                    .clientId(client.getId())
                    .invoiceNumber(invNum)
                    .invoiceDate(invoiceDate)
                    .dueDate(dueDate)
                    .subtotal(fee)
                    .tax(lineTax)
                    .total(total)
                    .paidAmount(status == InvoiceStatus.PAID ? total : BigDecimal.ZERO)
                    .balanceDue(status == InvoiceStatus.PAID ? BigDecimal.ZERO : total)
                    .status(status)
                    .terms("Payment due within 15 days of invoice date. Bank transfer / UPI details on invoice.")
                    .notes("Thank you for your business.")
                    .build();
            inv.setOrganizationId(organizationId);

            InvoiceItemEntity item = InvoiceItemEntity.builder()
                    .invoice(inv)
                    .service(svc)
                    .description(desc)
                    .quantity(BigDecimal.ONE)
                    .unitPrice(fee)
                    .taxRate(taxRate)
                    .tax(lineTax)
                    .amount(total)
                    .build();
            inv.setItems(List.of(item));

            InvoiceEntity saved = invoiceRepository.save(inv);

            if (status == InvoiceStatus.PAID) {
                InvoicePaymentEntity pmt = InvoicePaymentEntity.builder()
                        .invoice(saved)
                        .clientId(client.getId())
                        .amount(total)
                        .paymentDate(invoiceDate.plusDays(2))
                        .paymentMethod(InvoicePaymentEntity.PaymentMethod.BANK_TRANSFER)
                        .referenceNumber("NEFT" + (100000000 + i * 12345))
                        .notes("Full settlement via Bank Transfer")
                        .build();
                pmt.setOrganizationId(organizationId);
                invoicePaymentRepository.save(pmt);
            }

            seeded.add(enrichDto(saved));
        }

        return seeded;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private InvoiceDto enrichDto(InvoiceEntity entity) {
        if (entity == null) return null;
        InvoiceDto dto = invoiceMapper.toDto(entity);
        if (dto == null) return null;
        clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                .ifPresent(client -> {
                    dto.setClientName(client.getDisplayName());
                    dto.setClientGstin(client.getGstin());
                    dto.setClientPan(client.getPan());
                });

        if (entity.getItems() != null) {
            dto.setItems(invoiceMapper.toItemDtoList(entity.getItems()));
        }
        if (entity.getPayments() != null) {
            dto.setPayments(invoiceMapper.toPaymentDtoList(entity.getPayments()));
        }

        return dto;
    }

    private String generateInvoiceNumber(UUID organizationId) {
        int year = LocalDate.now().getYear();
        long count = invoiceRepository.countByOrganizationId(organizationId) + 1;
        String number;
        do {
            number = String.format("INV-%d-%04d", year, count++);
        } while (invoiceRepository.existsByOrganizationIdAndInvoiceNumber(organizationId, number));
        return number;
    }
}
