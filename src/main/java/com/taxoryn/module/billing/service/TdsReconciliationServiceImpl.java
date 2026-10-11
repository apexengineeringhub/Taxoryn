package com.taxoryn.module.billing.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.billing.dto.TdsCreditDto;
import com.taxoryn.module.billing.dto.TdsReconciliationItemDto;
import com.taxoryn.module.billing.dto.TdsReconciliationSummaryDto;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.TdsCreditEntity;
import com.taxoryn.module.billing.entity.TdsCreditEntity.TdsCreditSourceType;
import com.taxoryn.module.billing.entity.TdsCreditEntity.TdsCreditStatus;
import com.taxoryn.module.billing.entity.TdsCreditMatchEntity;
import com.taxoryn.module.billing.entity.TdsCreditMatchEntity.TdsMatchStatus;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.repository.TdsCreditMatchRepository;
import com.taxoryn.module.billing.repository.TdsCreditRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TdsReconciliationServiceImpl implements TdsReconciliationService {

    private final TdsCreditRepository tdsCreditRepository;
    private final TdsCreditMatchRepository tdsCreditMatchRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public TdsCreditDto recordTdsCredit(TdsCreditDto dto) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(dto.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", dto.getClientId()));

        String ay = dto.getAssessmentYear();
        if (ay == null || ay.isBlank()) {
            ay = calculateAyFromFy(dto.getFinancialYear());
        }

        TdsCreditEntity credit = TdsCreditEntity.builder()
                .clientId(client.getId())
                .deductorPan(dto.getDeductorPan() != null ? dto.getDeductorPan().toUpperCase() : (client.getPan() != null ? client.getPan() : "UNKNOWNPAN"))
                .deductorTan(dto.getDeductorTan())
                .deductorName(dto.getDeductorName() != null ? dto.getDeductorName() : client.getDisplayName())
                .sectionCode(dto.getSectionCode() != null ? dto.getSectionCode() : "194J")
                .financialYear(dto.getFinancialYear())
                .assessmentYear(ay)
                .quarter(dto.getQuarter())
                .grossAmount(dto.getGrossAmount() != null ? dto.getGrossAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .tdsDeducted(dto.getTdsDeducted() != null ? dto.getTdsDeducted().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .sourceType(dto.getSourceType() != null ? dto.getSourceType() : TdsCreditSourceType.MANUAL_ENTRY)
                .sourceDocumentRef(dto.getSourceDocumentRef())
                .certificateNumber(dto.getCertificateNumber())
                .certificateDate(dto.getCertificateDate())
                .creditStatus(TdsCreditStatus.UNRECONCILED)
                .notes(dto.getNotes())
                .build();
        credit.setOrganizationId(organizationId);

        TdsCreditEntity saved = tdsCreditRepository.save(credit);
        log.info("Recorded inward TDS credit: id={}, deductorPan={}, tdsDeducted={} for tenant={}",
                saved.getId(), saved.getDeductorPan(), saved.getTdsDeducted(), organizationId);

        return mapToDto(saved, client.getDisplayName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsCreditDto> getTdsCredits(UUID clientId, String financialYear) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<TdsCreditEntity> list = (clientId != null)
                ? tdsCreditRepository.findAllByOrganizationIdAndClientId(organizationId, clientId)
                : (financialYear != null)
                ? tdsCreditRepository.findAllByOrganizationIdAndFinancialYear(organizationId, financialYear)
                : tdsCreditRepository.findAllByOrganizationId(organizationId);

        Map<UUID, String> clientNames = clientRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, ClientEntity::getDisplayName, (a, b) -> a));

        return list.stream()
                .map(c -> mapToDto(c, clientNames.getOrDefault(c.getClientId(), "Practice Client")))
                .toList();
    }

    @Override
    @Transactional
    public TdsReconciliationSummaryDto runReconciliation(UUID clientId, String financialYear) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        log.info("Running automatic TDS credit reconciliation for tenant={}, client={}, fy={}", organizationId, clientId, financialYear);

        // Fetch Invoices (Exclude DRAFT and CANCELLED)
        List<InvoiceEntity> allInvoices = invoiceRepository.findAllByOrganizationId(organizationId).stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.DRAFT && inv.getStatus() != InvoiceStatus.CANCELLED)
                .filter(inv -> clientId == null || inv.getClientId().equals(clientId))
                .filter(inv -> financialYear == null || getFinancialYear(inv.getInvoiceDate()).equalsIgnoreCase(financialYear))
                .toList();

        // Fetch Inward TDS Credits
        List<TdsCreditEntity> allCredits = tdsCreditRepository.findAllByOrganizationId(organizationId).stream()
                .filter(c -> clientId == null || c.getClientId().equals(clientId))
                .filter(c -> financialYear == null || c.getFinancialYear().equalsIgnoreCase(financialYear))
                .toList();

        Map<UUID, ClientEntity> clientMap = clientRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, c -> c, (a, b) -> a));

        // Clear previous match records for this tenant/client scope to ensure deterministic idempotent execution
        List<TdsCreditMatchEntity> existingMatches = tdsCreditMatchRepository.findAllByOrganizationId(organizationId);
        if (clientId != null) {
            existingMatches = existingMatches.stream()
                    .filter(m -> {
                        InvoiceEntity inv = invoiceRepository.findByIdAndOrganizationId(m.getInvoiceId(), organizationId).orElse(null);
                        return inv != null && clientId.equals(inv.getClientId());
                    }).toList();
        }
        tdsCreditMatchRepository.deleteAll(existingMatches);

        Set<UUID> matchedCreditIds = new HashSet<>();
        List<TdsCreditMatchEntity> newMatches = new ArrayList<>();

        for (InvoiceEntity invoice : allInvoices) {
            ClientEntity client = clientMap.get(invoice.getClientId());
            String clientPan = client != null && client.getPan() != null ? client.getPan().toUpperCase().trim() : "";

            // Find candidates by client ID or deductor PAN
            List<TdsCreditEntity> candidates = allCredits.stream()
                    .filter(c -> !matchedCreditIds.contains(c.getId()))
                    .filter(c -> c.getClientId().equals(invoice.getClientId()) || (!clientPan.isEmpty() && clientPan.equalsIgnoreCase(c.getDeductorPan())))
                    .toList();

            TdsCreditEntity bestMatch = null;
            TdsMatchStatus status = null;
            BigDecimal confidence = BigDecimal.ZERO;
            String remarks = null;
            BigDecimal expectedRate = new BigDecimal("10.00");
            BigDecimal invoiceBase = invoice.getSubtotal() != null && invoice.getSubtotal().compareTo(BigDecimal.ZERO) > 0
                    ? invoice.getSubtotal()
                    : invoice.getTotal();

            for (TdsCreditEntity cand : candidates) {
                // Check section
                if ("194C".equalsIgnoreCase(cand.getSectionCode())) {
                    expectedRate = new BigDecimal("2.00");
                } else {
                    expectedRate = new BigDecimal("10.00");
                }

                BigDecimal calculatedExpectedTds = invoiceBase.multiply(expectedRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal baseDiff = invoiceBase.subtract(cand.getGrossAmount()).abs();
                BigDecimal tdsDiff = calculatedExpectedTds.subtract(cand.getTdsDeducted()).abs();

                // Exact match: base amount matches within 1.00 tolerance AND TDS deducted matches expected TDS
                if (baseDiff.compareTo(new BigDecimal("1.00")) <= 0 && tdsDiff.compareTo(new BigDecimal("1.00")) <= 0) {
                    bestMatch = cand;
                    status = TdsMatchStatus.EXACT_MATCH;
                    confidence = new BigDecimal("100.00");
                    remarks = "Perfect match on gross fee and TDS deduction rate (" + expectedRate + "% under Sec " + cand.getSectionCode() + ")";
                    break;
                }

                // Rate discrepancy: gross base matches, but deducted amount differs from expected statutory rate
                if (baseDiff.compareTo(new BigDecimal("1.00")) <= 0 && tdsDiff.compareTo(new BigDecimal("1.00")) > 0) {
                    bestMatch = cand;
                    status = TdsMatchStatus.RATE_DISCREPANCY;
                    confidence = new BigDecimal("85.00");
                    remarks = "Gross billing matches, but TDS deducted (₹" + cand.getTdsDeducted() + ") diverges from standard rate " + expectedRate + "% (₹" + calculatedExpectedTds + ")";
                    break;
                }

                // Amount mismatch: TDS deducted or gross partially aligned
                if (tdsDiff.compareTo(new BigDecimal("1.00")) <= 0) {
                    bestMatch = cand;
                    status = TdsMatchStatus.AMOUNT_MISMATCH;
                    confidence = new BigDecimal("70.00");
                    remarks = "TDS deduction amount matches, but gross certificate amount differs from invoice subtotal";
                    break;
                }
            }

            if (bestMatch != null) {
                matchedCreditIds.add(bestMatch.getId());
                BigDecimal calculatedExpectedTds = invoiceBase.multiply(expectedRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal rateDiscrepancy = calculatedExpectedTds.subtract(bestMatch.getTdsDeducted());

                TdsCreditMatchEntity match = TdsCreditMatchEntity.builder()
                        .invoiceId(invoice.getId())
                        .tdsCreditId(bestMatch.getId())
                        .matchStatus(status)
                        .matchConfidence(confidence)
                        .matchedAmount(bestMatch.getTdsDeducted())
                        .invoiceSubtotal(invoiceBase)
                        .creditGrossAmount(bestMatch.getGrossAmount())
                        .expectedTdsRate(expectedRate)
                        .expectedTdsAmount(calculatedExpectedTds)
                        .actualTdsDeducted(bestMatch.getTdsDeducted())
                        .rateDiscrepancy(rateDiscrepancy)
                        .remarks(remarks)
                        .reconciledAt(Instant.now())
                        .reconciledBy(SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId().toString() : "SYSTEM")
                        .build();
                match.setOrganizationId(organizationId);
                newMatches.add(match);

                bestMatch.setCreditStatus(status == TdsMatchStatus.EXACT_MATCH ? TdsCreditStatus.RECONCILED : TdsCreditStatus.DISCREPANCY);
                tdsCreditRepository.save(bestMatch);
            }
        }

        if (!newMatches.isEmpty()) {
            tdsCreditMatchRepository.saveAll(newMatches);
        }

        return getReconciliationSummary(clientId, financialYear);
    }

    @Override
    @Transactional(readOnly = true)
    public TdsReconciliationSummaryDto getReconciliationSummary(UUID clientId, String financialYear) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        List<InvoiceEntity> invoices = invoiceRepository.findAllByOrganizationId(organizationId).stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.DRAFT && inv.getStatus() != InvoiceStatus.CANCELLED)
                .filter(inv -> clientId == null || inv.getClientId().equals(clientId))
                .filter(inv -> financialYear == null || getFinancialYear(inv.getInvoiceDate()).equalsIgnoreCase(financialYear))
                .toList();

        List<TdsCreditEntity> credits = tdsCreditRepository.findAllByOrganizationId(organizationId).stream()
                .filter(c -> clientId == null || c.getClientId().equals(clientId))
                .filter(c -> financialYear == null || c.getFinancialYear().equalsIgnoreCase(financialYear))
                .toList();

        List<TdsCreditMatchEntity> matches = tdsCreditMatchRepository.findAllByOrganizationId(organizationId);

        Map<UUID, InvoiceEntity> invoiceMap = invoices.stream()
                .collect(Collectors.toMap(InvoiceEntity::getId, inv -> inv, (a, b) -> a));
        Map<UUID, TdsCreditEntity> creditMap = credits.stream()
                .collect(Collectors.toMap(TdsCreditEntity::getId, c -> c, (a, b) -> a));
        Map<UUID, ClientEntity> clientMap = clientRepository.findAllByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, c -> c, (a, b) -> a));

        List<TdsReconciliationItemDto> matchedItems = new ArrayList<>();
        List<TdsReconciliationItemDto> discrepancyItems = new ArrayList<>();
        Set<UUID> matchedInvoiceIds = new HashSet<>();
        Set<UUID> matchedCreditIds = new HashSet<>();

        BigDecimal totalGrossInvoiced = invoices.stream().map(InvoiceEntity::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCashReceived = invoices.stream().map(InvoiceEntity::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReceivableBalance = invoices.stream().map(InvoiceEntity::getBalanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTdsReported = credits.stream().map(TdsCreditEntity::getTdsDeducted).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTdsMatched = BigDecimal.ZERO;
        BigDecimal totalTdsDiscrepancy = BigDecimal.ZERO;

        for (TdsCreditMatchEntity m : matches) {
            InvoiceEntity inv = invoiceMap.get(m.getInvoiceId());
            TdsCreditEntity cr = creditMap.get(m.getTdsCreditId());
            if (inv == null || cr == null) continue;

            matchedInvoiceIds.add(inv.getId());
            matchedCreditIds.add(cr.getId());

            ClientEntity cl = clientMap.get(inv.getClientId());
            String clientName = cl != null ? cl.getDisplayName() : "Practice Client";
            String clientPan = cl != null ? cl.getPan() : "";

            TdsReconciliationItemDto item = TdsReconciliationItemDto.builder()
                    .matchId(m.getId())
                    .invoiceId(inv.getId())
                    .invoiceNumber(inv.getInvoiceNumber())
                    .invoiceDate(inv.getInvoiceDate())
                    .invoiceSubtotal(inv.getSubtotal())
                    .invoiceTotal(inv.getTotal())
                    .cashReceived(inv.getPaidAmount())
                    .receivableBalance(inv.getBalanceDue())
                    .invoiceStatus(inv.getStatus().name())
                    .clientId(inv.getClientId())
                    .clientName(clientName)
                    .clientPan(clientPan)
                    .tdsCreditId(cr.getId())
                    .deductorPan(cr.getDeductorPan())
                    .deductorTan(cr.getDeductorTan())
                    .deductorName(cr.getDeductorName())
                    .sectionCode(cr.getSectionCode())
                    .financialYear(cr.getFinancialYear())
                    .quarter(cr.getQuarter())
                    .creditGrossAmount(cr.getGrossAmount())
                    .actualTdsDeducted(cr.getTdsDeducted())
                    .expectedTdsRate(m.getExpectedTdsRate())
                    .expectedTdsAmount(m.getExpectedTdsAmount())
                    .rateDiscrepancy(m.getRateDiscrepancy())
                    .sourceType(cr.getSourceType())
                    .certificateNumber(cr.getCertificateNumber())
                    .matchStatus(m.getMatchStatus())
                    .matchConfidence(m.getMatchConfidence())
                    .remarks(m.getRemarks())
                    .reconciledAt(m.getReconciledAt())
                    .build();

            if (m.getMatchStatus() == TdsMatchStatus.EXACT_MATCH) {
                matchedItems.add(item);
                totalTdsMatched = totalTdsMatched.add(cr.getTdsDeducted());
            } else {
                discrepancyItems.add(item);
                totalTdsDiscrepancy = totalTdsDiscrepancy.add(cr.getTdsDeducted());
            }
        }

        // Unmatched Invoices
        List<TdsReconciliationItemDto> unmatchedInvoices = new ArrayList<>();
        for (InvoiceEntity inv : invoices) {
            if (!matchedInvoiceIds.contains(inv.getId())) {
                ClientEntity cl = clientMap.get(inv.getClientId());
                unmatchedInvoices.add(TdsReconciliationItemDto.builder()
                        .invoiceId(inv.getId())
                        .invoiceNumber(inv.getInvoiceNumber())
                        .invoiceDate(inv.getInvoiceDate())
                        .invoiceSubtotal(inv.getSubtotal())
                        .invoiceTotal(inv.getTotal())
                        .cashReceived(inv.getPaidAmount())
                        .receivableBalance(inv.getBalanceDue())
                        .invoiceStatus(inv.getStatus().name())
                        .clientId(inv.getClientId())
                        .clientName(cl != null ? cl.getDisplayName() : "Practice Client")
                        .clientPan(cl != null ? cl.getPan() : "")
                        .matchStatus(TdsMatchStatus.UNMATCHED_INVOICE)
                        .remarks("No attributable TDS credit record found for client PAN / billing period")
                        .build());
            }
        }

        // Unmatched Credits
        List<TdsCreditDto> unmatchedCredits = credits.stream()
                .filter(cr -> !matchedCreditIds.contains(cr.getId()))
                .map(cr -> mapToDto(cr, clientMap.containsKey(cr.getClientId()) ? clientMap.get(cr.getClientId()).getDisplayName() : "Practice Client"))
                .toList();

        BigDecimal totalTdsUnmatched = unmatchedCredits.stream().map(TdsCreditDto::getTdsDeducted).reduce(BigDecimal.ZERO, BigDecimal::add);

        return TdsReconciliationSummaryDto.builder()
                .totalGrossInvoiced(totalGrossInvoiced)
                .totalCashReceived(totalCashReceived)
                .totalReceivableBalance(totalReceivableBalance)
                .totalTdsReported(totalTdsReported)
                .totalTdsMatched(totalTdsMatched)
                .totalTdsUnmatched(totalTdsUnmatched)
                .totalTdsDiscrepancy(totalTdsDiscrepancy)
                .totalInvoicesCount(invoices.size())
                .matchedCount(matchedItems.size())
                .discrepancyCount(discrepancyItems.size())
                .unmatchedInvoicesCount(unmatchedInvoices.size())
                .unmatchedCreditsCount(unmatchedCredits.size())
                .matchedItems(matchedItems)
                .discrepancyItems(discrepancyItems)
                .unmatchedInvoices(unmatchedInvoices)
                .unmatchedCredits(unmatchedCredits)
                .build();
    }

    private TdsCreditDto mapToDto(TdsCreditEntity entity, String clientName) {
        return TdsCreditDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .deductorPan(entity.getDeductorPan())
                .deductorTan(entity.getDeductorTan())
                .deductorName(entity.getDeductorName())
                .sectionCode(entity.getSectionCode())
                .financialYear(entity.getFinancialYear())
                .assessmentYear(entity.getAssessmentYear())
                .quarter(entity.getQuarter())
                .grossAmount(entity.getGrossAmount())
                .tdsDeducted(entity.getTdsDeducted())
                .sourceType(entity.getSourceType())
                .sourceDocumentRef(entity.getSourceDocumentRef())
                .certificateNumber(entity.getCertificateNumber())
                .certificateDate(entity.getCertificateDate())
                .creditStatus(entity.getCreditStatus())
                .notes(entity.getNotes())
                .build();
    }

    private String getFinancialYear(LocalDate date) {
        if (date == null) return "2025-26";
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month >= 4) {
            return year + "-" + String.format("%02d", (year + 1) % 100);
        } else {
            return (year - 1) + "-" + String.format("%02d", year % 100);
        }
    }

    private String calculateAyFromFy(String fy) {
        if (fy == null || !fy.contains("-")) return "2026-27";
        try {
            String[] parts = fy.split("-");
            int startYear = Integer.parseInt(parts[0].trim());
            return (startYear + 1) + "-" + String.format("%02d", (startYear + 2) % 100);
        } catch (Exception ex) {
            return "2026-27";
        }
    }
}
