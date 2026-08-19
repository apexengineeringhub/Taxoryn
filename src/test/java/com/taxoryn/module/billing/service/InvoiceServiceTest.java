package com.taxoryn.module.billing.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.BillingDashboardStatsDto;
import com.taxoryn.module.billing.dto.ClientBillingHistoryDto;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.dto.InvoicePaymentDto;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity.PaymentMethod;
import com.taxoryn.module.billing.mapper.InvoiceMapper;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.portal.entity.ClientNotificationEntity;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientNotificationRepository notificationRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private UUID tenantId;
    private UUID clientId;
    private UUID invoiceId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(tenantId)
                .email("ca@apexpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("BILLING_CREATE", "BILLING_VIEW", "BILLING_UPDATE", "BILLING_DELETE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create invoice calculates line pricing, 18% tax, subtotal, total, and DRAFT status")
    void testCreateInvoiceCalculations() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .pan("AAACB1234D")
                .gstin("27AAACB1234D1Z5")
                .build();
        client.setId(clientId);

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .clientId(clientId)
                .invoiceDate(LocalDate.of(2026, 8, 20))
                .dueDate(LocalDate.of(2026, 9, 5))
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("GSTR-1 & GSTR-3B Monthly Filing")
                                .quantity(new BigDecimal("1.00"))
                                .unitPrice(new BigDecimal("2000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build(),
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.ITR_FILING)
                                .description("ITR-6 Corporate Filing")
                                .quantity(new BigDecimal("1.00"))
                                .unitPrice(new BigDecimal("8000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .notes("Thank you for your business")
                .build();

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(invoiceRepository.countByOrganizationId(tenantId)).thenReturn(0L);
        when(invoiceRepository.existsByOrganizationIdAndInvoiceNumber(any(), any())).thenReturn(false);

        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> {
            InvoiceEntity entity = invocation.getArgument(0);
            entity.setId(invoiceId);
            return entity;
        });

        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenAnswer(invocation -> {
            InvoiceEntity entity = invocation.getArgument(0);
            return InvoiceDto.builder()
                    .id(entity.getId())
                    .invoiceNumber(entity.getInvoiceNumber())
                    .subtotal(entity.getSubtotal())
                    .tax(entity.getTax())
                    .total(entity.getTotal())
                    .balanceDue(entity.getBalanceDue())
                    .status(entity.getStatus())
                    .build();
        });

        InvoiceDto result = invoiceService.createInvoice(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("10000.00"), result.getSubtotal()); // 2000 + 8000
        assertEquals(new BigDecimal("1800.00"), result.getTax());       // 18% of 10000
        assertEquals(new BigDecimal("11800.00"), result.getTotal());    // 10000 + 1800
        assertEquals(new BigDecimal("11800.00"), result.getBalanceDue());
        assertEquals(InvoiceStatus.DRAFT, result.getStatus());
    }

    @Test
    @DisplayName("Issue invoice transitions DRAFT to ISSUED and generates client notification")
    void testIssueInvoice() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(clientId)
                .invoiceNumber("INV-2026-0001")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .subtotal(new BigDecimal("5000.00"))
                .tax(new BigDecimal("900.00"))
                .total(new BigDecimal("5900.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("5900.00"))
                .status(InvoiceStatus.DRAFT)
                .build();
        invoice.setId(invoiceId);
        invoice.setOrganizationId(tenantId);

        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(InvoiceDto.builder()
                .id(invoiceId)
                .status(InvoiceStatus.ISSUED)
                .build());

        InvoiceDto result = invoiceService.issueInvoice(invoiceId);

        assertNotNull(result);
        assertEquals(InvoiceStatus.ISSUED, result.getStatus());
        verify(notificationRepository).save(any(ClientNotificationEntity.class));
    }

    @Test
    @DisplayName("Record partial payment updates paid amount, balance due, and sets PARTIALLY_PAID status")
    void testRecordPartialPayment() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(clientId)
                .invoiceNumber("INV-2026-0001")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .total(new BigDecimal("10000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("10000.00"))
                .status(InvoiceStatus.ISSUED)
                .build();
        invoice.setId(invoiceId);
        invoice.setOrganizationId(tenantId);

        RecordPaymentRequest paymentRequest = RecordPaymentRequest.builder()
                .amount(new BigDecimal("4000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.UPI)
                .referenceNumber("UPI-123456789")
                .build();

        InvoicePaymentEntity savedPayment = InvoicePaymentEntity.builder()
                .invoice(invoice)
                .clientId(clientId)
                .amount(new BigDecimal("4000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        savedPayment.setId(UUID.randomUUID());

        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(invoicePaymentRepository.save(any(InvoicePaymentEntity.class))).thenReturn(savedPayment);
        when(invoiceMapper.toPaymentDto(any(InvoicePaymentEntity.class))).thenReturn(InvoicePaymentDto.builder()
                .id(savedPayment.getId())
                .amount(new BigDecimal("4000.00"))
                .paymentMethod(PaymentMethod.UPI)
                .build());

        InvoicePaymentDto result = invoiceService.recordPayment(invoiceId, paymentRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("4000.00"), result.getAmount());
        assertEquals(new BigDecimal("4000.00"), invoice.getPaidAmount());
        assertEquals(new BigDecimal("6000.00"), invoice.getBalanceDue());
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
    }

    @Test
    @DisplayName("Record full payment updates balance due to 0 and sets PAID status")
    void testRecordFullPayment() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(clientId)
                .invoiceNumber("INV-2026-0001")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .total(new BigDecimal("5000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("5000.00"))
                .status(InvoiceStatus.ISSUED)
                .build();
        invoice.setId(invoiceId);
        invoice.setOrganizationId(tenantId);

        RecordPaymentRequest paymentRequest = RecordPaymentRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .referenceNumber("NEFT-998877")
                .build();

        InvoicePaymentEntity savedPayment = InvoicePaymentEntity.builder()
                .invoice(invoice)
                .clientId(clientId)
                .amount(new BigDecimal("5000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();
        savedPayment.setId(UUID.randomUUID());

        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(invoicePaymentRepository.save(any(InvoicePaymentEntity.class))).thenReturn(savedPayment);
        when(invoiceMapper.toPaymentDto(any(InvoicePaymentEntity.class))).thenReturn(InvoicePaymentDto.builder()
                .id(savedPayment.getId())
                .amount(new BigDecimal("5000.00"))
                .build());

        InvoicePaymentDto result = invoiceService.recordPayment(invoiceId, paymentRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("5000.00"), invoice.getPaidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getBalanceDue()));
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    @DisplayName("Cancel invoice on already PAID invoice throws BadRequestException")
    void testCancelPaidInvoiceThrowsBadRequest() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .status(InvoiceStatus.PAID)
                .build();
        invoice.setId(invoiceId);

        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));

        assertThrows(BadRequestException.class, () -> invoiceService.cancelInvoice(invoiceId));
    }

    @Test
    @DisplayName("Get client billing history aggregates total billed, total paid, and outstanding balances")
    void testGetClientBillingHistory() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .pan("AAACB1234D")
                .gstin("27AAACB1234D1Z5")
                .build();
        client.setId(clientId);

        InvoiceEntity inv1 = InvoiceEntity.builder()
                .total(new BigDecimal("10000.00"))
                .paidAmount(new BigDecimal("10000.00"))
                .balanceDue(BigDecimal.ZERO)
                .status(InvoiceStatus.PAID)
                .invoiceDate(LocalDate.now().minusMonths(1))
                .dueDate(LocalDate.now().minusDays(10))
                .build();
        inv1.setId(UUID.randomUUID());

        InvoiceEntity inv2 = InvoiceEntity.builder()
                .total(new BigDecimal("5000.00"))
                .paidAmount(new BigDecimal("2000.00"))
                .balanceDue(new BigDecimal("3000.00"))
                .status(InvoiceStatus.PARTIALLY_PAID)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().minusDays(2)) // overdue
                .build();
        inv2.setId(UUID.randomUUID());

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(invoiceRepository.findAllByOrganizationIdAndClientIdOrderByInvoiceDateDesc(tenantId, clientId))
                .thenReturn(List.of(inv2, inv1));
        when(invoicePaymentRepository.findAllByOrganizationIdAndClientIdOrderByPaymentDateDesc(tenantId, clientId))
                .thenReturn(List.of());
        when(invoiceMapper.toPaymentDtoList(any())).thenReturn(List.of());
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenAnswer(inv -> {
            InvoiceEntity e = inv.getArgument(0);
            return InvoiceDto.builder()
                    .id(e.getId())
                    .status(e.getStatus())
                    .total(e.getTotal())
                    .paidAmount(e.getPaidAmount())
                    .balanceDue(e.getBalanceDue())
                    .build();
        });

        ClientBillingHistoryDto history = invoiceService.getClientBillingHistory(clientId);

        assertNotNull(history);
        assertEquals("ABC Traders", history.getClientName());
        assertEquals(new BigDecimal("15000.00"), history.getTotalBilled());
        assertEquals(new BigDecimal("12000.00"), history.getTotalPaid());
        assertEquals(new BigDecimal("3000.00"), history.getTotalOutstanding());
        assertEquals(2, history.getTotalInvoicesCount());
        assertEquals(1, history.getPaidInvoicesCount());
        assertEquals(1, history.getOverdueInvoicesCount());
    }

    @Test
    @DisplayName("Get billing dashboard stats aggregates practice totals and service revenue")
    void testGetBillingDashboardStats() {
        InvoiceItemEntity item1 = InvoiceItemEntity.builder()
                .service(BillingServiceType.GST_FILING)
                .amount(new BigDecimal("2360.00"))
                .build();

        InvoiceEntity inv1 = InvoiceEntity.builder()
                .total(new BigDecimal("2360.00"))
                .paidAmount(new BigDecimal("2360.00"))
                .balanceDue(BigDecimal.ZERO)
                .status(InvoiceStatus.PAID)
                .dueDate(LocalDate.now().plusDays(10))
                .items(new ArrayList<>(List.of(item1)))
                .build();

        InvoiceEntity inv2 = InvoiceEntity.builder()
                .total(new BigDecimal("5000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("5000.00"))
                .status(InvoiceStatus.DRAFT)
                .dueDate(LocalDate.now().plusDays(15))
                .items(new ArrayList<>())
                .build();

        when(invoiceRepository.findAllByOrganizationId(tenantId)).thenReturn(List.of(inv1, inv2));

        BillingDashboardStatsDto stats = invoiceService.getBillingDashboardStats();

        assertNotNull(stats);
        assertEquals(new BigDecimal("2360.00"), stats.getTotalBilled());
        assertEquals(new BigDecimal("2360.00"), stats.getTotalCollected());
        assertEquals(BigDecimal.ZERO, stats.getTotalOutstanding());
        assertEquals(new BigDecimal("5000.00"), stats.getTotalDraft());
        assertEquals(2, stats.getTotalInvoices());
        assertEquals(1, stats.getPaidInvoices());
        assertEquals(1, stats.getDraftInvoices());
        assertEquals(new BigDecimal("2360.00"), stats.getRevenueByService().get("GST_FILING"));
    }
}
