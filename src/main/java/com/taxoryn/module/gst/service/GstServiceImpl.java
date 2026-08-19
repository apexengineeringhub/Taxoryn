package com.taxoryn.module.gst.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.dto.BatchGenerateFilingsRequest;
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.CreateGstReturnFilingRequest;
import com.taxoryn.module.gst.dto.GstFilingFilterRequest;
import com.taxoryn.module.gst.dto.GstMonthlySummaryDto;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstProfileFilterRequest;
import com.taxoryn.module.gst.dto.GstReturnFilingDto;
import com.taxoryn.module.gst.dto.GstWorkloadDashboardDto;
import com.taxoryn.module.gst.dto.GstWorkloadDashboardDto.GstClientWorkloadItem;
import com.taxoryn.module.gst.dto.SaveGstMonthlySummaryRequest;
import com.taxoryn.module.gst.dto.UpdateGstFilingStatusRequest;
import com.taxoryn.module.gst.dto.UpdateGstProfileRequest;
import com.taxoryn.module.gst.dto.UpdateGstProfileStatusRequest;
import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity;
import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity.ChallanStatus;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus;
import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.mapper.GstMapper;
import com.taxoryn.module.gst.repository.GstMonthlySummaryRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GstServiceImpl implements GstService {

    private final GstProfileRepository gstProfileRepository;
    private final GstReturnFilingRepository gstReturnFilingRepository;
    private final GstMonthlySummaryRepository gstMonthlySummaryRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final GstMapper gstMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;

    // =========================================================================
    // 1. Profile Management
    // =========================================================================

    @Override
    @Transactional
    public GstProfileDto createProfile(CreateGstProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        String formattedGstin = request.getGstin().toUpperCase().trim();
        if (gstProfileRepository.existsByOrganizationIdAndGstin(organizationId, formattedGstin)) {
            throw new DuplicateResourceException("GST Profile", "gstin", formattedGstin);
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        String stateCode = StringUtils.hasText(request.getStateCode())
                ? request.getStateCode().trim()
                : (formattedGstin.length() >= 2 ? formattedGstin.substring(0, 2) : "27");

        GstProfileEntity entity = GstProfileEntity.builder()
                .clientId(request.getClientId())
                .gstin(formattedGstin)
                .legalName(StringUtils.hasText(request.getLegalName()) ? request.getLegalName().trim() : client.getLegalName())
                .tradeName(StringUtils.hasText(request.getTradeName()) ? request.getTradeName().trim() : client.getDisplayName())
                .gstType(request.getGstType() != null ? request.getGstType() : GstType.REGULAR)
                .filingFrequency(request.getFilingFrequency())
                .registrationDate(request.getRegistrationDate())
                .stateCode(stateCode)
                .principalPlaceOfBusiness(request.getPrincipalPlaceOfBusiness())
                .assignedEmployeeId(request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId() : client.getAssignedEmployeeId())
                .status(request.getStatus() != null ? request.getStatus() : GstProfileStatus.ACTIVE)
                .build();
        entity.setOrganizationId(organizationId);

        GstProfileEntity saved = gstProfileRepository.save(entity);

        // Update client GSTIN if not set
        if (!StringUtils.hasText(client.getGstin())) {
            client.setGstin(formattedGstin);
            clientRepository.save(client);
        }

        log.info("Created GST Profile: id={}, gstin={} for tenant={}", saved.getId(), saved.getGstin(), organizationId);
        GstProfileDto result = enrichProfileDto(saved);
        auditService.logEvent("GST_PROFILE_CREATED", "GST_PROFILE", saved.getId().toString(), null, result);
        return result;
    }

    @Override
    @Transactional
    public GstProfileDto updateProfile(UUID id, UpdateGstProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        GstProfileEntity profile = gstProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Profile", "id", id));

        GstProfileDto oldSnapshot = enrichProfileDto(profile);

        String newGstin = request.getGstin().toUpperCase().trim();
        if (!newGstin.equalsIgnoreCase(profile.getGstin())
                && gstProfileRepository.existsByOrganizationIdAndGstin(organizationId, newGstin)) {
            throw new DuplicateResourceException("GST Profile", "gstin", newGstin);
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        profile.setGstin(newGstin);
        profile.setLegalName(request.getLegalName());
        profile.setTradeName(request.getTradeName());
        profile.setGstType(request.getGstType());
        profile.setFilingFrequency(request.getFilingFrequency());
        profile.setRegistrationDate(request.getRegistrationDate());
        profile.setStateCode(StringUtils.hasText(request.getStateCode()) ? request.getStateCode().trim() : (newGstin.length() >= 2 ? newGstin.substring(0, 2) : "27"));
        profile.setPrincipalPlaceOfBusiness(request.getPrincipalPlaceOfBusiness());
        profile.setAssignedEmployeeId(request.getAssignedEmployeeId());
        if (request.getStatus() != null) {
            profile.setStatus(request.getStatus());
        }

        GstProfileEntity saved = gstProfileRepository.save(profile);
        log.info("Updated GST Profile: id={} for tenant={}", saved.getId(), organizationId);
        GstProfileDto result = enrichProfileDto(saved);
        auditService.logEvent("GST_PROFILE_UPDATED", "GST_PROFILE", saved.getId().toString(), oldSnapshot, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GstProfileDto getProfileById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        GstProfileEntity profile = gstProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Profile", "id", id));

        return enrichProfileDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GstProfileDto> getProfiles(GstProfileFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<GstProfileEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("gstin")), pattern),
                        cb.like(cb.lower(root.get("legalName")), pattern),
                        cb.like(cb.lower(root.get("tradeName")), pattern),
                        cb.like(cb.lower(root.get("stateCode")), pattern)
                ));
            }

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }

            if (filterRequest.getGstType() != null) {
                predicates.add(cb.equal(root.get("gstType"), filterRequest.getGstType()));
            }

            if (filterRequest.getFilingFrequency() != null) {
                predicates.add(cb.equal(root.get("filingFrequency"), filterRequest.getFilingFrequency()));
            }

            if (filterRequest.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filterRequest.getAssignedEmployeeId()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GstProfileEntity> page = gstProfileRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichProfileDto);
    }

    @Override
    @Transactional
    public GstProfileDto updateProfileStatus(UUID id, UpdateGstProfileStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        GstProfileEntity profile = gstProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Profile", "id", id));

        GstProfileStatus oldStatus = profile.getStatus();
        profile.setStatus(request.getStatus());
        GstProfileEntity saved = gstProfileRepository.save(profile);
        log.info("Updated GST Profile status: id={}, status={} for tenant={}", id, request.getStatus(), organizationId);
        GstProfileDto result = enrichProfileDto(saved);
        auditService.logEvent("GST_PROFILE_STATUS_UPDATED", "GST_PROFILE", id.toString(), oldStatus != null ? oldStatus.name() : null, request.getStatus().name());
        return result;
    }

    // =========================================================================
    // 2. Return Filings Management
    // =========================================================================

    @Override
    @Transactional
    public GstReturnFilingDto createFiling(CreateGstReturnFilingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        GstProfileEntity profile = gstProfileRepository.findByIdAndOrganizationId(request.getGstProfileId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Profile", "id", request.getGstProfileId()));

        if (gstReturnFilingRepository.existsByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
                organizationId, request.getGstProfileId(), request.getReturnType(), request.getReturnPeriod())) {
            throw new DuplicateResourceException("GST Return Filing", "returnType & period",
                    request.getReturnType() + " for " + request.getReturnPeriod());
        }

        UUID assignedEmpId = request.getAssignedEmployeeId() != null
                ? request.getAssignedEmployeeId()
                : profile.getAssignedEmployeeId();

        GstReturnFilingEntity filing = GstReturnFilingEntity.builder()
                .gstProfileId(profile.getId())
                .clientId(profile.getClientId())
                .returnType(request.getReturnType())
                .returnPeriod(request.getReturnPeriod())
                .financialYear(request.getFinancialYear())
                .dueDate(request.getDueDate())
                .filingStatus(request.getFilingStatus() != null ? request.getFilingStatus() : GstFilingStatus.PENDING)
                .totalTaxableValue(request.getTotalTaxableValue() != null ? request.getTotalTaxableValue() : BigDecimal.ZERO)
                .totalTaxLiability(request.getTotalTaxLiability() != null ? request.getTotalTaxLiability() : BigDecimal.ZERO)
                .totalItcClaimed(request.getTotalItcClaimed() != null ? request.getTotalItcClaimed() : BigDecimal.ZERO)
                .taxPaidCash(request.getTaxPaidCash() != null ? request.getTaxPaidCash() : BigDecimal.ZERO)
                .taxPaidItc(request.getTaxPaidItc() != null ? request.getTaxPaidItc() : BigDecimal.ZERO)
                .assignedEmployeeId(assignedEmpId)
                .notes(request.getNotes())
                .build();
        filing.setOrganizationId(organizationId);

        GstReturnFilingEntity saved = gstReturnFilingRepository.save(filing);
        log.info("Created GST filing: id={}, type={}, period={} for tenant={}", saved.getId(), saved.getReturnType(), saved.getReturnPeriod(), organizationId);
        GstReturnFilingDto result = enrichFilingDto(saved, profile);
        auditService.logEvent("GST_FILING_CREATED", "GST_FILING", saved.getId().toString(), null, result);
        return result;
    }

    @Override
    @Transactional
    public GstReturnFilingDto updateFilingStatus(UUID id, UpdateGstFilingStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        GstReturnFilingEntity filing = gstReturnFilingRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Return Filing", "id", id));

        GstFilingStatus oldStatus = filing.getFilingStatus();

        filing.setFilingStatus(request.getFilingStatus());
        if (request.getFilingDate() != null) {
            filing.setFilingDate(request.getFilingDate());
        } else if (request.getFilingStatus() == GstFilingStatus.FILED && filing.getFilingDate() == null) {
            filing.setFilingDate(LocalDate.now());
        }

        if (StringUtils.hasText(request.getAcknowledgementNumber())) {
            filing.setAcknowledgementNumber(request.getAcknowledgementNumber().trim());
        }
        if (request.getTotalTaxableValue() != null) {
            filing.setTotalTaxableValue(request.getTotalTaxableValue());
        }
        if (request.getTotalTaxLiability() != null) {
            filing.setTotalTaxLiability(request.getTotalTaxLiability());
        }
        if (request.getTotalItcClaimed() != null) {
            filing.setTotalItcClaimed(request.getTotalItcClaimed());
        }
        if (request.getTaxPaidCash() != null) {
            filing.setTaxPaidCash(request.getTaxPaidCash());
        }
        if (request.getTaxPaidItc() != null) {
            filing.setTaxPaidItc(request.getTaxPaidItc());
        }
        if (StringUtils.hasText(request.getNotes())) {
            filing.setNotes(request.getNotes());
        }

        GstReturnFilingEntity saved = gstReturnFilingRepository.save(filing);
        log.info("Updated GST filing status: id={}, newStatus={} for tenant={}", saved.getId(), saved.getFilingStatus(), organizationId);
        GstReturnFilingDto result = enrichFilingDto(saved, null);
        auditService.logEvent("GST_FILING_STATUS_UPDATED", "GST_FILING", saved.getId().toString(), oldStatus != null ? oldStatus.name() : null, saved.getFilingStatus().name());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GstReturnFilingDto getFilingById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        GstReturnFilingEntity filing = gstReturnFilingRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Return Filing", "id", id));

        return enrichFilingDto(filing, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GstReturnFilingDto> getFilings(GstFilingFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<GstReturnFilingEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (filterRequest.getGstProfileId() != null) {
                predicates.add(cb.equal(root.get("gstProfileId"), filterRequest.getGstProfileId()));
            }
            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }
            if (filterRequest.getReturnType() != null) {
                predicates.add(cb.equal(root.get("returnType"), filterRequest.getReturnType()));
            }
            if (StringUtils.hasText(filterRequest.getReturnPeriod())) {
                predicates.add(cb.equal(root.get("returnPeriod"), filterRequest.getReturnPeriod().trim()));
            }
            if (StringUtils.hasText(filterRequest.getFinancialYear())) {
                predicates.add(cb.equal(root.get("financialYear"), filterRequest.getFinancialYear().trim()));
            }
            if (filterRequest.getFilingStatus() != null) {
                predicates.add(cb.equal(root.get("filingStatus"), filterRequest.getFilingStatus()));
            }
            if (filterRequest.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filterRequest.getAssignedEmployeeId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GstReturnFilingEntity> page = gstReturnFilingRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, f -> enrichFilingDto(f, null));
    }

    @Override
    @Transactional
    public List<GstReturnFilingDto> batchGenerateFilings(BatchGenerateFilingsRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<GstProfileEntity> activeProfiles = gstProfileRepository.findAllByOrganizationIdAndStatus(organizationId, GstProfileStatus.ACTIVE);

        List<GstReturnFilingDto> createdFilings = new ArrayList<>();

        for (GstProfileEntity profile : activeProfiles) {
            for (GstReturnType returnType : request.getReturnTypes()) {
                // If profile is composition, skip GSTR-1/3B unless requested; if regular, skip CMP-08
                if (profile.getGstType() == GstType.COMPOSITION && (returnType == GstReturnType.GSTR1 || returnType == GstReturnType.GSTR3B)) {
                    continue;
                }
                if (profile.getGstType() == GstType.REGULAR && returnType == GstReturnType.CMP08) {
                    continue;
                }

                if (!gstReturnFilingRepository.existsByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
                        organizationId, profile.getId(), returnType, request.getReturnPeriod())) {

                    LocalDate dueDate = null;
                    if (returnType == GstReturnType.GSTR1) {
                        dueDate = request.getGstr1DueDate();
                    } else if (returnType == GstReturnType.GSTR3B) {
                        dueDate = request.getGstr3bDueDate();
                    } else if (returnType == GstReturnType.CMP08) {
                        dueDate = request.getCmp08DueDate();
                    }

                    GstReturnFilingEntity filing = GstReturnFilingEntity.builder()
                            .gstProfileId(profile.getId())
                            .clientId(profile.getClientId())
                            .returnType(returnType)
                            .returnPeriod(request.getReturnPeriod())
                            .financialYear(request.getFinancialYear())
                            .dueDate(dueDate)
                            .filingStatus(GstFilingStatus.PENDING)
                            .assignedEmployeeId(profile.getAssignedEmployeeId())
                            .build();
                    filing.setOrganizationId(organizationId);

                    GstReturnFilingEntity saved = gstReturnFilingRepository.save(filing);
                    createdFilings.add(enrichFilingDto(saved, profile));
                }
            }
        }

        log.info("Batch generated {} GST return filings for period {} in tenant {}", createdFilings.size(), request.getReturnPeriod(), organizationId);
        auditService.logEvent("GST_FILING_BATCH_GENERATED", "GST_FILING", request.getReturnPeriod(), null, "Generated " + createdFilings.size() + " filings");
        return createdFilings;
    }

    // =========================================================================
    // 3. Monthly Computation & Summary
    // =========================================================================

    @Override
    @Transactional
    public GstMonthlySummaryDto saveMonthlySummary(SaveGstMonthlySummaryRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        GstProfileEntity profile = gstProfileRepository.findByIdAndOrganizationId(request.getGstProfileId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Profile", "id", request.getGstProfileId()));

        Optional<GstMonthlySummaryEntity> existing = gstMonthlySummaryRepository.findByOrganizationIdAndGstProfileIdAndPeriod(
                organizationId, request.getGstProfileId(), request.getPeriod());

        GstMonthlySummaryEntity entity = existing.orElseGet(() -> GstMonthlySummaryEntity.builder()
                .gstProfileId(profile.getId())
                .clientId(profile.getClientId())
                .period(request.getPeriod())
                .financialYear(request.getFinancialYear())
                .build());

        entity.setFinancialYear(request.getFinancialYear());
        entity.setTotalSalesTaxable(request.getTotalSalesTaxable() != null ? request.getTotalSalesTaxable() : BigDecimal.ZERO);
        entity.setIgstSales(request.getIgstSales() != null ? request.getIgstSales() : BigDecimal.ZERO);
        entity.setCgstSales(request.getCgstSales() != null ? request.getCgstSales() : BigDecimal.ZERO);
        entity.setSgstSales(request.getSgstSales() != null ? request.getSgstSales() : BigDecimal.ZERO);
        entity.setCessSales(request.getCessSales() != null ? request.getCessSales() : BigDecimal.ZERO);

        entity.setTotalPurchaseTaxable(request.getTotalPurchaseTaxable() != null ? request.getTotalPurchaseTaxable() : BigDecimal.ZERO);
        entity.setIgstPurchase(request.getIgstPurchase() != null ? request.getIgstPurchase() : BigDecimal.ZERO);
        entity.setCgstPurchase(request.getCgstPurchase() != null ? request.getCgstPurchase() : BigDecimal.ZERO);
        entity.setSgstPurchase(request.getSgstPurchase() != null ? request.getSgstPurchase() : BigDecimal.ZERO);
        entity.setCessPurchase(request.getCessPurchase() != null ? request.getCessPurchase() : BigDecimal.ZERO);

        entity.setItcEligible(request.getItcEligible() != null ? request.getItcEligible() : BigDecimal.ZERO);
        entity.setItcIneligible(request.getItcIneligible() != null ? request.getItcIneligible() : BigDecimal.ZERO);
        entity.setItcReversed(request.getItcReversed() != null ? request.getItcReversed() : BigDecimal.ZERO);
        entity.setItcNetClaimed(request.getItcNetClaimed() != null ? request.getItcNetClaimed() : entity.getItcEligible().subtract(entity.getItcReversed()));

        // Calculate net liability if not explicitly provided
        BigDecimal totalTaxOutput = entity.getIgstSales().add(entity.getCgstSales()).add(entity.getSgstSales()).add(entity.getCessSales());
        BigDecimal netLiability = request.getNetTaxLiability() != null
                ? request.getNetTaxLiability()
                : totalTaxOutput.subtract(entity.getItcNetClaimed()).max(BigDecimal.ZERO);
        entity.setNetTaxLiability(netLiability);

        if (request.getChallanStatus() != null) {
            entity.setChallanStatus(request.getChallanStatus());
        }
        if (StringUtils.hasText(request.getChallanCprn())) {
            entity.setChallanCprn(request.getChallanCprn().trim());
        }
        if (StringUtils.hasText(request.getNotes())) {
            entity.setNotes(request.getNotes().trim());
        }

        if (entity.getOrganizationId() == null) {
            entity.setOrganizationId(organizationId);
        }

        GstMonthlySummaryEntity saved = gstMonthlySummaryRepository.save(entity);
        log.info("Saved GST monthly summary: id={}, profile={}, period={} for tenant={}", saved.getId(), profile.getId(), request.getPeriod(), organizationId);
        GstMonthlySummaryDto result = enrichSummaryDto(saved, profile);
        auditService.logEvent("GST_SUMMARY_SAVED", "GST_SUMMARY", saved.getId().toString(), existing.isPresent() ? "UPDATED" : "CREATED", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GstMonthlySummaryDto getMonthlySummary(UUID gstProfileId, String period) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        GstProfileEntity profile = gstProfileRepository.findByIdAndOrganizationId(gstProfileId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Profile", "id", gstProfileId));

        GstMonthlySummaryEntity entity = gstMonthlySummaryRepository.findByOrganizationIdAndGstProfileIdAndPeriod(
                organizationId, gstProfileId, period)
                .orElseThrow(() -> new ResourceNotFoundException("GST Monthly Summary", "period", period));

        return enrichSummaryDto(entity, profile);
    }

    // =========================================================================
    // 4. Workload Dashboard & History
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public GstWorkloadDashboardDto getWorkloadDashboard(String period, UUID assignedEmployeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        List<GstProfileEntity> profiles = gstProfileRepository.findAllByOrganizationIdAndStatus(organizationId, GstProfileStatus.ACTIVE);

        if (assignedEmployeeId != null) {
            profiles = profiles.stream()
                    .filter(p -> assignedEmployeeId.equals(p.getAssignedEmployeeId()))
                    .toList();
        }

        List<GstClientWorkloadItem> clientItems = new ArrayList<>();

        long gstr1Pending = 0;
        long gstr1Filed = 0;
        long gstr3bPending = 0;
        long gstr3bFiled = 0;
        BigDecimal totalItc = BigDecimal.ZERO;
        BigDecimal totalLiability = BigDecimal.ZERO;

        for (GstProfileEntity profile : profiles) {
            ClientEntity client = clientRepository.findByIdAndOrganizationId(profile.getClientId(), organizationId).orElse(null);
            String clientName = client != null ? client.getDisplayName() : profile.getTradeName();

            String assignedTo = "Unassigned";
            if (profile.getAssignedEmployeeId() != null) {
                Optional<EmployeeEntity> emp = employeeRepository.findByIdAndOrganizationId(profile.getAssignedEmployeeId(), organizationId);
                if (emp.isPresent()) {
                    assignedTo = emp.get().getFullName();
                }
            }

            // Filings lookup
            Optional<GstReturnFilingEntity> gstr1 = gstReturnFilingRepository.findByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
                    organizationId, profile.getId(), GstReturnType.GSTR1, period);
            Optional<GstReturnFilingEntity> gstr3b = gstReturnFilingRepository.findByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
                    organizationId, profile.getId(), GstReturnType.GSTR3B, period);
            Optional<GstReturnFilingEntity> cmp08 = gstReturnFilingRepository.findByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
                    organizationId, profile.getId(), GstReturnType.CMP08, period);

            GstFilingStatus gstr1Status = gstr1.map(GstReturnFilingEntity::getFilingStatus).orElse(GstFilingStatus.PENDING);
            GstFilingStatus gstr3bStatus = gstr3b.map(GstReturnFilingEntity::getFilingStatus).orElse(GstFilingStatus.PENDING);
            GstFilingStatus cmp08Status = cmp08.map(GstReturnFilingEntity::getFilingStatus).orElse(null);

            if (gstr1Status == GstFilingStatus.FILED) {
                gstr1Filed++;
            } else {
                gstr1Pending++;
            }

            if (gstr3bStatus == GstFilingStatus.FILED) {
                gstr3bFiled++;
            } else {
                gstr3bPending++;
            }

            // Summary lookup for ITC & Liability
            Optional<GstMonthlySummaryEntity> summary = gstMonthlySummaryRepository.findByOrganizationIdAndGstProfileIdAndPeriod(
                    organizationId, profile.getId(), period);

            BigDecimal itc = summary.map(GstMonthlySummaryEntity::getItcNetClaimed).orElse(BigDecimal.ZERO);
            BigDecimal liability = summary.map(GstMonthlySummaryEntity::getNetTaxLiability).orElse(BigDecimal.ZERO);

            totalItc = totalItc.add(itc);
            totalLiability = totalLiability.add(liability);

            LocalDate dueDate = gstr3b.map(GstReturnFilingEntity::getDueDate)
                    .orElseGet(() -> gstr1.map(GstReturnFilingEntity::getDueDate).orElse(null));

            String overallStatus = "PENDING";
            if (gstr1Status == GstFilingStatus.FILED && gstr3bStatus == GstFilingStatus.FILED) {
                overallStatus = "FILED";
            } else if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
                overallStatus = "OVERDUE";
            }

            clientItems.add(GstClientWorkloadItem.builder()
                    .clientId(profile.getClientId())
                    .clientName(clientName)
                    .gstProfileId(profile.getId())
                    .gstin(profile.getGstin())
                    .gstType(profile.getGstType())
                    .period(formatPeriodLabel(period))
                    .gstr1Status(gstr1Status)
                    .gstr3bStatus(gstr3bStatus)
                    .cmp08Status(cmp08Status)
                    .itc(itc)
                    .taxLiability(liability)
                    .dueDate(dueDate)
                    .assignedEmployeeId(profile.getAssignedEmployeeId())
                    .assignedTo(assignedTo)
                    .overallStatus(overallStatus)
                    .build());
        }

        return GstWorkloadDashboardDto.builder()
                .period(period)
                .periodLabel(formatPeriodLabel(period))
                .totalGstClients(profiles.size())
                .gstr1PendingCount(gstr1Pending)
                .gstr1FiledCount(gstr1Filed)
                .gstr3bPendingCount(gstr3bPending)
                .gstr3bFiledCount(gstr3bFiled)
                .totalItcTracked(totalItc)
                .totalTaxLiability(totalLiability)
                .clients(clientItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GstReturnFilingDto> getClientFilingHistory(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        List<GstReturnFilingEntity> filings = gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(organizationId, clientId);
        return filings.stream().map(f -> enrichFilingDto(f, null)).toList();
    }

    // =========================================================================
    // Helpers & Enrichers
    // =========================================================================

    private GstProfileDto enrichProfileDto(GstProfileEntity entity) {
        GstProfileDto dto = gstMapper.toDto(entity);
        clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                .ifPresent(client -> dto.setClientName(client.getDisplayName()));

        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getAssignedEmployeeId(), entity.getOrganizationId())
                    .ifPresent(emp -> dto.setAssignedEmployeeName(emp.getFullName()));
        }
        return dto;
    }

    private GstReturnFilingDto enrichFilingDto(GstReturnFilingEntity entity, GstProfileEntity profile) {
        GstReturnFilingDto dto = gstMapper.toFilingDto(entity);

        if (profile == null) {
            profile = gstProfileRepository.findByIdAndOrganizationId(entity.getGstProfileId(), entity.getOrganizationId()).orElse(null);
        }
        if (profile != null) {
            dto.setGstin(profile.getGstin());
        }

        clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                .ifPresent(client -> dto.setClientName(client.getDisplayName()));

        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getAssignedEmployeeId(), entity.getOrganizationId())
                    .ifPresent(emp -> dto.setAssignedEmployeeName(emp.getFullName()));
        }
        return dto;
    }

    private GstMonthlySummaryDto enrichSummaryDto(GstMonthlySummaryEntity entity, GstProfileEntity profile) {
        GstMonthlySummaryDto dto = gstMapper.toSummaryDto(entity);

        if (profile == null) {
            profile = gstProfileRepository.findByIdAndOrganizationId(entity.getGstProfileId(), entity.getOrganizationId()).orElse(null);
        }
        if (profile != null) {
            dto.setGstin(profile.getGstin());
        }

        clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                .ifPresent(client -> dto.setClientName(client.getDisplayName()));

        return dto;
    }

    private String formatPeriodLabel(String period) {
        if (!StringUtils.hasText(period)) {
            return "Current Period";
        }
        try {
            if (period.matches("^\\d{4}-\\d{2}$")) {
                YearMonth ym = YearMonth.parse(period);
                return ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            }
        } catch (Exception ignored) {
        }
        return period;
    }
}
