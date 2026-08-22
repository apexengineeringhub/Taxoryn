package com.taxoryn.module.tds.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.tds.dto.*;
import com.taxoryn.module.tds.entity.*;
import com.taxoryn.module.tds.entity.TdsChallanEntity.ChallanStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.FvuValidationStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import com.taxoryn.module.tds.mapper.TdsMapper;
import com.taxoryn.module.tds.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TdsServiceImpl implements TdsService {

    private final TdsProfileRepository tdsProfileRepository;
    private final TdsReturnRepository tdsReturnRepository;
    private final TdsChallanRepository tdsChallanRepository;
    private final TdsDeducteeEntryRepository tdsDeducteeEntryRepository;
    private final TdsCertificateRepository tdsCertificateRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TdsMapper tdsMapper;
    private final TdsCalculatorService tdsCalculatorService;
    private final AuditService auditService;

    // =========================================================================
    // 1. TDS Profiles (TAN Master)
    // =========================================================================

    @Override
    @Transactional
    public TdsProfileDto createProfile(CreateTdsProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        String formattedTan = request.getTan().toUpperCase().trim();

        if (tdsProfileRepository.findByOrganizationIdAndTan(organizationId, formattedTan).isPresent()) {
            throw new DuplicateResourceException("TDS Profile", "tan", formattedTan);
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        if (StringUtils.hasText(client.getTan()) && !client.getTan().equalsIgnoreCase(formattedTan)) {
            client.setTan(formattedTan);
            clientRepository.save(client);
        } else if (!StringUtils.hasText(client.getTan())) {
            client.setTan(formattedTan);
            clientRepository.save(client);
        }

        TdsProfileEntity entity = TdsProfileEntity.builder()
                .clientId(request.getClientId())
                .tan(formattedTan)
                .deductorType(request.getDeductorType())
                .branchDivisionName(request.getBranchDivisionName())
                .paCode(request.getPaCode())
                .ddoCode(request.getDdoCode())
                .ministryName(request.getMinistryName())
                .responsiblePersonName(request.getResponsiblePersonName())
                .responsiblePersonPan(request.getResponsiblePersonPan() != null ? request.getResponsiblePersonPan().toUpperCase().trim() : null)
                .responsiblePersonDesignation(request.getResponsiblePersonDesignation())
                .responsiblePersonFatherName(request.getResponsiblePersonFatherName())
                .responsiblePersonEmail(request.getResponsiblePersonEmail())
                .responsiblePersonMobile(request.getResponsiblePersonMobile())
                .responsiblePersonAddress(request.getResponsiblePersonAddress())
                .assignedEmployeeId(request.getAssignedEmployeeId())
                .status(request.getStatus() != null ? request.getStatus() : TdsProfileEntity.TdsProfileStatus.ACTIVE)
                .tracesUsername(request.getTracesUsername())
                .tracesStatus(request.getTracesStatus() != null ? request.getTracesStatus() : TdsProfileEntity.TracesStatus.NOT_REGISTERED)
                .build();
        entity.setOrganizationId(organizationId);

        TdsProfileEntity saved = tdsProfileRepository.save(entity);
        log.info("Created TDS Profile ID: {} for TAN: {} in Organization: {}", saved.getId(), formattedTan, organizationId);
        auditService.logEvent("TDS_PROFILE_CREATED", "TDS_PROFILE", saved.getId().toString(), null, "TDS Profile registered for TAN " + formattedTan);

        return enrichProfileDto(tdsMapper.toProfileDto(saved), client, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TdsProfileDto> getProfiles(TdsProfileFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<TdsProfileEntity> page = tdsProfileRepository.searchProfiles(
                organizationId,
                filterRequest.getClientId(),
                filterRequest.getDeductorType(),
                filterRequest.getStatus(),
                filterRequest.getSearch(),
                filterRequest.toPageable()
        );

        return PagedResponse.of(page, this::enrichProfileEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public TdsProfileDto getProfileById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsProfileEntity entity = tdsProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Profile", "id", id));
        return enrichProfileEntity(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TdsProfileDto getProfileByClientId(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsProfileEntity entity = tdsProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Profile", "clientId", clientId));
        return enrichProfileEntity(entity);
    }

    @Override
    @Transactional
    public TdsProfileDto updateProfile(UUID id, UpdateTdsProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsProfileEntity entity = tdsProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Profile", "id", id));

        if (request.getDeductorType() != null) entity.setDeductorType(request.getDeductorType());
        if (request.getBranchDivisionName() != null) entity.setBranchDivisionName(request.getBranchDivisionName());
        if (request.getPaCode() != null) entity.setPaCode(request.getPaCode());
        if (request.getDdoCode() != null) entity.setDdoCode(request.getDdoCode());
        if (request.getMinistryName() != null) entity.setMinistryName(request.getMinistryName());
        if (request.getResponsiblePersonName() != null) entity.setResponsiblePersonName(request.getResponsiblePersonName());
        if (request.getResponsiblePersonPan() != null) entity.setResponsiblePersonPan(request.getResponsiblePersonPan().toUpperCase().trim());
        if (request.getResponsiblePersonDesignation() != null) entity.setResponsiblePersonDesignation(request.getResponsiblePersonDesignation());
        if (request.getResponsiblePersonFatherName() != null) entity.setResponsiblePersonFatherName(request.getResponsiblePersonFatherName());
        if (request.getResponsiblePersonEmail() != null) entity.setResponsiblePersonEmail(request.getResponsiblePersonEmail());
        if (request.getResponsiblePersonMobile() != null) entity.setResponsiblePersonMobile(request.getResponsiblePersonMobile());
        if (request.getResponsiblePersonAddress() != null) entity.setResponsiblePersonAddress(request.getResponsiblePersonAddress());
        if (request.getAssignedEmployeeId() != null) entity.setAssignedEmployeeId(request.getAssignedEmployeeId());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getTracesUsername() != null) entity.setTracesUsername(request.getTracesUsername());
        if (request.getTracesStatus() != null) entity.setTracesStatus(request.getTracesStatus());

        TdsProfileEntity updated = tdsProfileRepository.save(entity);
        auditService.logEvent("TDS_PROFILE_UPDATED", "TDS_PROFILE", updated.getId().toString(), null, "TDS Profile updated for TAN " + updated.getTan());

        return enrichProfileEntity(updated);
    }

    @Override
    @Transactional
    public BulkTdsProfileImportResultDto bulkCreateProfiles(List<CreateTdsProfileRequest> requests) {
        BulkTdsProfileImportResultDto result = BulkTdsProfileImportResultDto.builder()
                .totalProcessed(requests != null ? requests.size() : 0)
                .build();

        if (requests == null || requests.isEmpty()) return result;

        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<TdsProfileDto> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (CreateTdsProfileRequest req : requests) {
            try {
                TdsProfileDto dto = createProfile(req);
                imported.add(dto);
                created++;
            } catch (DuplicateResourceException e) {
                skipped++;
            } catch (Exception e) {
                failed++;
                errors.add("TAN " + req.getTan() + ": " + e.getMessage());
            }
        }

        result.setTotalCreated(created);
        result.setTotalSkipped(skipped);
        result.setTotalFailed(failed);
        result.setImportedProfiles(imported);
        result.setErrorMessages(errors);

        return result;
    }

    // =========================================================================
    // 2. TDS Returns Lifecycle
    // =========================================================================

    @Override
    @Transactional
    public TdsReturnDto createReturn(CreateTdsReturnRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Optional<TdsReturnEntity> existing = tdsReturnRepository.findByOrganizationIdAndTdsProfileIdAndFormTypeAndQuarterAndFinancialYear(
                organizationId,
                request.getTdsProfileId(),
                request.getFormType(),
                request.getQuarter(),
                request.getFinancialYear()
        );

        if (existing.isPresent()) {
            throw new DuplicateResourceException("TDS Return", "formType/quarter/fy", request.getFormType() + " / " + request.getQuarter() + " / " + request.getFinancialYear());
        }

        TdsProfileEntity profile = tdsProfileRepository.findByIdAndOrganizationId(request.getTdsProfileId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Profile", "id", request.getTdsProfileId()));

        LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : calculateQuarterlyDueDate(request.getQuarter(), request.getFinancialYear());
        String ay = StringUtils.hasText(request.getAssessmentYear()) ? request.getAssessmentYear() : computeAssessmentYear(request.getFinancialYear());

        TdsReturnEntity entity = TdsReturnEntity.builder()
                .clientId(request.getClientId())
                .tdsProfileId(request.getTdsProfileId())
                .formType(request.getFormType())
                .quarter(request.getQuarter())
                .financialYear(request.getFinancialYear())
                .assessmentYear(ay)
                .dueDate(dueDate)
                .filingStatus(request.getFilingStatus() != null ? request.getFilingStatus() : TdsFilingStatus.PENDING)
                .filingDate(request.getFilingDate())
                .tokenNumber(request.getTokenNumber())
                .receiptNumber(request.getReceiptNumber())
                .totalAmountPaid(request.getTotalAmountPaid() != null ? request.getTotalAmountPaid() : BigDecimal.ZERO)
                .totalTaxDeducted(request.getTotalTaxDeducted() != null ? request.getTotalTaxDeducted() : BigDecimal.ZERO)
                .totalTaxDeposited(request.getTotalTaxDeposited() != null ? request.getTotalTaxDeposited() : BigDecimal.ZERO)
                .totalInterest(request.getTotalInterest() != null ? request.getTotalInterest() : BigDecimal.ZERO)
                .totalLateFee(request.getTotalLateFee() != null ? request.getTotalLateFee() : BigDecimal.ZERO)
                .totalPenalty(request.getTotalPenalty() != null ? request.getTotalPenalty() : BigDecimal.ZERO)
                .assignedEmployeeId(request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId() : profile.getAssignedEmployeeId())
                .fvuValidationStatus(request.getFvuValidationStatus() != null ? request.getFvuValidationStatus() : FvuValidationStatus.NOT_VALIDATED)
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        TdsReturnEntity saved = tdsReturnRepository.save(entity);
        log.info("Created TDS Return ID: {} Form: {} Quarter: {} FY: {}", saved.getId(), saved.getFormType(), saved.getQuarter(), saved.getFinancialYear());
        auditService.logEvent("TDS_RETURN_CREATED", "TDS_RETURN", saved.getId().toString(), null, "TDS Return scheduled for Form " + saved.getFormType() + " " + saved.getQuarter() + " FY " + saved.getFinancialYear());

        return enrichReturnEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TdsReturnDto> getReturns(TdsReturnFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<TdsReturnEntity> page = tdsReturnRepository.searchReturns(
                organizationId,
                filterRequest.getClientId(),
                filterRequest.getTdsProfileId(),
                filterRequest.getFormType(),
                filterRequest.getQuarter(),
                filterRequest.getFinancialYear(),
                filterRequest.getFilingStatus(),
                filterRequest.getAssignedEmployeeId(),
                filterRequest.toPageable()
        );

        return PagedResponse.of(page, this::enrichReturnEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public TdsReturnDto getReturnById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsReturnEntity entity = tdsReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Return", "id", id));
        return enrichReturnEntity(entity);
    }

    @Override
    @Transactional
    public TdsReturnDto updateReturn(UUID id, UpdateTdsReturnRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsReturnEntity entity = tdsReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Return", "id", id));

        if (request.getDueDate() != null) entity.setDueDate(request.getDueDate());
        if (request.getFilingStatus() != null) entity.setFilingStatus(request.getFilingStatus());
        if (request.getFilingDate() != null) entity.setFilingDate(request.getFilingDate());
        if (request.getTokenNumber() != null) entity.setTokenNumber(request.getTokenNumber());
        if (request.getReceiptNumber() != null) entity.setReceiptNumber(request.getReceiptNumber());
        if (request.getTotalAmountPaid() != null) entity.setTotalAmountPaid(request.getTotalAmountPaid());
        if (request.getTotalTaxDeducted() != null) entity.setTotalTaxDeducted(request.getTotalTaxDeducted());
        if (request.getTotalTaxDeposited() != null) entity.setTotalTaxDeposited(request.getTotalTaxDeposited());
        if (request.getTotalInterest() != null) entity.setTotalInterest(request.getTotalInterest());
        if (request.getTotalLateFee() != null) entity.setTotalLateFee(request.getTotalLateFee());
        if (request.getTotalPenalty() != null) entity.setTotalPenalty(request.getTotalPenalty());
        if (request.getAssignedEmployeeId() != null) entity.setAssignedEmployeeId(request.getAssignedEmployeeId());
        if (request.getFvuValidationStatus() != null) entity.setFvuValidationStatus(request.getFvuValidationStatus());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());

        TdsReturnEntity updated = tdsReturnRepository.save(entity);
        auditService.logEvent("TDS_RETURN_UPDATED", "TDS_RETURN", updated.getId().toString(), null, "TDS Return updated: " + updated.getFormType() + " " + updated.getQuarter());

        return enrichReturnEntity(updated);
    }

    @Override
    @Transactional
    public TdsReturnDto updateStatus(UUID id, UpdateTdsReturnStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsReturnEntity entity = tdsReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Return", "id", id));

        entity.setFilingStatus(request.getFilingStatus());
        if (request.getFilingDate() != null) entity.setFilingDate(request.getFilingDate());
        if (request.getTokenNumber() != null) entity.setTokenNumber(request.getTokenNumber());
        if (request.getReceiptNumber() != null) entity.setReceiptNumber(request.getReceiptNumber());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());

        TdsReturnEntity updated = tdsReturnRepository.save(entity);
        auditService.logEvent("TDS_STATUS_UPDATED", "TDS_RETURN", updated.getId().toString(), null, "TDS Return status transitioned to " + request.getFilingStatus());

        return enrichReturnEntity(updated);
    }

    @Override
    @Transactional
    public TdsReturnDto recordFiling(UUID id, RecordTdsFilingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsReturnEntity entity = tdsReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Return", "id", id));

        entity.setFilingStatus(TdsFilingStatus.FILED);
        entity.setFilingDate(request.getFilingDate());
        entity.setTokenNumber(request.getTokenNumber());
        if (request.getReceiptNumber() != null) entity.setReceiptNumber(request.getReceiptNumber());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());

        TdsReturnEntity updated = tdsReturnRepository.save(entity);
        auditService.logEvent("TDS_RETURN_FILED", "TDS_RETURN", updated.getId().toString(), null, "TDS Return marked as FILED with Token " + request.getTokenNumber());

        return enrichReturnEntity(updated);
    }

    @Override
    @Transactional
    public TdsReturnDto assignEmployee(UUID id, AssignTdsEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsReturnEntity entity = tdsReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Return", "id", id));

        EmployeeEntity employee = employeeRepository.findByIdAndOrganizationId(request.getEmployeeId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        entity.setAssignedEmployeeId(employee.getId());
        TdsReturnEntity updated = tdsReturnRepository.save(entity);
        auditService.logEvent("TDS_EMPLOYEE_ASSIGNED", "TDS_RETURN", updated.getId().toString(), null, "Assigned employee " + employee.getFullName() + " to TDS return");

        return enrichReturnEntity(updated);
    }

    @Override
    @Transactional
    public List<TdsReturnDto> batchGenerateReturns(BatchGenerateTdsReturnsRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<TdsProfileEntity> activeProfiles = tdsProfileRepository.findAllByOrganizationIdAndStatus(organizationId, TdsProfileEntity.TdsProfileStatus.ACTIVE);

        List<TdsFormType> targetForms = (request.getFormTypes() != null && !request.getFormTypes().isEmpty())
                ? request.getFormTypes()
                : List.of(TdsFormType.FORM_24Q, TdsFormType.FORM_26Q);

        LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : calculateQuarterlyDueDate(request.getQuarter(), request.getFinancialYear());
        String ay = StringUtils.hasText(request.getAssessmentYear()) ? request.getAssessmentYear() : computeAssessmentYear(request.getFinancialYear());

        List<TdsReturnEntity> createdReturns = new ArrayList<>();

        for (TdsProfileEntity profile : activeProfiles) {
            for (TdsFormType form : targetForms) {
                Optional<TdsReturnEntity> existing = tdsReturnRepository.findByOrganizationIdAndTdsProfileIdAndFormTypeAndQuarterAndFinancialYear(
                        organizationId, profile.getId(), form, request.getQuarter(), request.getFinancialYear()
                );

                if (existing.isEmpty()) {
                    TdsReturnEntity entity = TdsReturnEntity.builder()
                            .clientId(profile.getClientId())
                            .tdsProfileId(profile.getId())
                            .formType(form)
                            .quarter(request.getQuarter())
                            .financialYear(request.getFinancialYear())
                            .assessmentYear(ay)
                            .dueDate(dueDate)
                            .filingStatus(TdsFilingStatus.PENDING)
                            .assignedEmployeeId(profile.getAssignedEmployeeId())
                            .fvuValidationStatus(FvuValidationStatus.NOT_VALIDATED)
                            .build();
                    entity.setOrganizationId(organizationId);
                    createdReturns.add(entity);
                }
            }
        }

        List<TdsReturnEntity> savedAll = tdsReturnRepository.saveAll(createdReturns);
        log.info("Batch generated {} TDS returns for Quarter {} FY {}", savedAll.size(), request.getQuarter(), request.getFinancialYear());
        auditService.logEvent("TDS_BATCH_GENERATION", "TDS_RETURN", "", null, "Batch generated " + savedAll.size() + " returns for " + request.getQuarter() + " " + request.getFinancialYear());

        return savedAll.stream().map(this::enrichReturnEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BulkTdsReturnImportResultDto bulkCreateReturns(List<CreateTdsReturnRequest> requests) {
        BulkTdsReturnImportResultDto result = BulkTdsReturnImportResultDto.builder()
                .totalProcessed(requests != null ? requests.size() : 0)
                .build();

        if (requests == null || requests.isEmpty()) return result;

        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<TdsReturnDto> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (CreateTdsReturnRequest req : requests) {
            try {
                TdsReturnDto dto = createReturn(req);
                imported.add(dto);
                created++;
            } catch (DuplicateResourceException e) {
                skipped++;
            } catch (Exception e) {
                failed++;
                errors.add("Form " + req.getFormType() + " " + req.getQuarter() + ": " + e.getMessage());
            }
        }

        result.setTotalCreated(created);
        result.setTotalSkipped(skipped);
        result.setTotalFailed(failed);
        result.setImportedReturns(imported);
        result.setErrorMessages(errors);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsReturnDto> getUpcomingReturns(int daysAhead) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate threshold = LocalDate.now().plusDays(daysAhead);
        return tdsReturnRepository.findUpcomingReturns(organizationId, threshold).stream()
                .map(this::enrichReturnEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsReturnDto> getOverdueReturns() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return tdsReturnRepository.findOverdueReturns(organizationId, LocalDate.now()).stream()
                .map(this::enrichReturnEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsReturnDto> getClientReturnHistory(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return tdsReturnRepository.findAllByOrganizationIdAndClientId(organizationId, clientId).stream()
                .map(this::enrichReturnEntity)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 3. Challans ITNS 281
    // =========================================================================

    @Override
    @Transactional
    public TdsChallanDto createChallan(CreateTdsChallanRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        TdsProfileEntity profile = tdsProfileRepository.findByIdAndOrganizationId(request.getTdsProfileId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Profile", "id", request.getTdsProfileId()));

        BigDecimal tds = request.getTdsAmount() != null ? request.getTdsAmount() : BigDecimal.ZERO;
        BigDecimal surcharge = request.getSurchargeAmount() != null ? request.getSurchargeAmount() : BigDecimal.ZERO;
        BigDecimal cess = request.getCessAmount() != null ? request.getCessAmount() : BigDecimal.ZERO;
        BigDecimal interest = request.getInterestAmount() != null ? request.getInterestAmount() : BigDecimal.ZERO;
        BigDecimal fee = request.getFeeAmount() != null ? request.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal penalty = request.getPenaltyAmount() != null ? request.getPenaltyAmount() : BigDecimal.ZERO;
        BigDecimal total = tds.add(surcharge).add(cess).add(interest).add(fee).add(penalty);

        String cin = StringUtils.hasText(request.getCin())
                ? request.getCin()
                : request.getBsrCode() + request.getChallanDate().toString().replace("-", "") + request.getChallanSerialNo();

        TdsChallanEntity entity = TdsChallanEntity.builder()
                .tdsProfileId(request.getTdsProfileId())
                .tdsReturnId(request.getTdsReturnId())
                .bsrCode(request.getBsrCode())
                .challanDate(request.getChallanDate())
                .challanSerialNo(request.getChallanSerialNo())
                .cin(cin)
                .majorHead(request.getMajorHead())
                .minorHead(request.getMinorHead())
                .sectionCode(request.getSectionCode().toUpperCase())
                .tdsAmount(tds)
                .surchargeAmount(surcharge)
                .cessAmount(cess)
                .interestAmount(interest)
                .feeAmount(fee)
                .penaltyAmount(penalty)
                .totalAmount(total)
                .utilizedAmount(BigDecimal.ZERO)
                .balanceAmount(total)
                .challanStatus(ChallanStatus.UNUTILIZED)
                .quarter(request.getQuarter())
                .financialYear(request.getFinancialYear())
                .paymentMode(request.getPaymentMode())
                .bankName(request.getBankName())
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        TdsChallanEntity saved = tdsChallanRepository.save(entity);
        auditService.logEvent("TDS_CHALLAN_CREATED", "TDS_CHALLAN", saved.getId().toString(), null, "Recorded Challan 281 CIN: " + cin + " Total: ₹" + total);

        return enrichChallanEntity(saved, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TdsChallanDto> getChallans(TdsChallanFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<TdsChallanEntity> page = tdsChallanRepository.searchChallans(
                organizationId,
                filterRequest.getTdsProfileId(),
                filterRequest.getQuarter(),
                filterRequest.getFinancialYear(),
                filterRequest.getChallanStatus(),
                filterRequest.getSectionCode(),
                filterRequest.getSearch(),
                filterRequest.toPageable()
        );

        return PagedResponse.of(page, this::enrichChallanEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public TdsChallanDto getChallanById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsChallanEntity entity = tdsChallanRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Challan", "id", id));
        return enrichChallanEntity(entity);
    }

    @Override
    @Transactional
    public TdsChallanDto updateChallan(UUID id, UpdateTdsChallanRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsChallanEntity entity = tdsChallanRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Challan", "id", id));

        if (request.getTdsReturnId() != null) entity.setTdsReturnId(request.getTdsReturnId());
        if (request.getBsrCode() != null) entity.setBsrCode(request.getBsrCode());
        if (request.getChallanDate() != null) entity.setChallanDate(request.getChallanDate());
        if (request.getChallanSerialNo() != null) entity.setChallanSerialNo(request.getChallanSerialNo());
        if (request.getMajorHead() != null) entity.setMajorHead(request.getMajorHead());
        if (request.getMinorHead() != null) entity.setMinorHead(request.getMinorHead());
        if (request.getSectionCode() != null) entity.setSectionCode(request.getSectionCode().toUpperCase());
        if (request.getTdsAmount() != null) entity.setTdsAmount(request.getTdsAmount());
        if (request.getSurchargeAmount() != null) entity.setSurchargeAmount(request.getSurchargeAmount());
        if (request.getCessAmount() != null) entity.setCessAmount(request.getCessAmount());
        if (request.getInterestAmount() != null) entity.setInterestAmount(request.getInterestAmount());
        if (request.getFeeAmount() != null) entity.setFeeAmount(request.getFeeAmount());
        if (request.getPenaltyAmount() != null) entity.setPenaltyAmount(request.getPenaltyAmount());

        BigDecimal total = entity.getTdsAmount().add(entity.getSurchargeAmount()).add(entity.getCessAmount())
                .add(entity.getInterestAmount()).add(entity.getFeeAmount()).add(entity.getPenaltyAmount());
        entity.setTotalAmount(total);

        if (request.getUtilizedAmount() != null) {
            entity.setUtilizedAmount(request.getUtilizedAmount());
            entity.setBalanceAmount(total.subtract(request.getUtilizedAmount()));
            if (entity.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                entity.setChallanStatus(ChallanStatus.FULLY_UTILIZED);
            } else if (entity.getUtilizedAmount().compareTo(BigDecimal.ZERO) > 0) {
                entity.setChallanStatus(ChallanStatus.PARTIALLY_UTILIZED);
            }
        }
        if (request.getChallanStatus() != null) entity.setChallanStatus(request.getChallanStatus());
        if (request.getPaymentMode() != null) entity.setPaymentMode(request.getPaymentMode());
        if (request.getBankName() != null) entity.setBankName(request.getBankName());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());

        TdsChallanEntity updated = tdsChallanRepository.save(entity);
        return enrichChallanEntity(updated);
    }

    // =========================================================================
    // 4. Deductee Register
    // =========================================================================

    @Override
    @Transactional
    public TdsDeducteeEntryDto createDeducteeEntry(CreateTdsDeducteeEntryRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        TdsProfileEntity profile = tdsProfileRepository.findByIdAndOrganizationId(request.getTdsProfileId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Profile", "id", request.getTdsProfileId()));

        BigDecimal totalDeducted = request.getTdsAmount()
                .add(request.getSurchargeAmount() != null ? request.getSurchargeAmount() : BigDecimal.ZERO)
                .add(request.getCessAmount() != null ? request.getCessAmount() : BigDecimal.ZERO);

        TdsDeducteeEntryEntity entity = TdsDeducteeEntryEntity.builder()
                .tdsProfileId(request.getTdsProfileId())
                .tdsReturnId(request.getTdsReturnId())
                .challanId(request.getChallanId())
                .deducteePan(request.getDeducteePan().toUpperCase().trim())
                .deducteeName(request.getDeducteeName())
                .deducteeType(request.getDeducteeType())
                .sectionCode(request.getSectionCode().toUpperCase())
                .paymentCreditDate(request.getPaymentCreditDate())
                .invoiceRefNumber(request.getInvoiceRefNumber())
                .amountPaidCredited(request.getAmountPaidCredited())
                .tdsRate(request.getTdsRate())
                .tdsAmount(request.getTdsAmount())
                .surchargeAmount(request.getSurchargeAmount() != null ? request.getSurchargeAmount() : BigDecimal.ZERO)
                .cessAmount(request.getCessAmount() != null ? request.getCessAmount() : BigDecimal.ZERO)
                .totalTaxDeducted(totalDeducted)
                .deductionDate(request.getDeductionDate())
                .certificateNumber197(request.getCertificateNumber197())
                .reasonCode(request.getReasonCode())
                .quarter(request.getQuarter())
                .financialYear(request.getFinancialYear())
                .status(TdsDeducteeEntryEntity.DeducteeEntryStatus.ACTIVE)
                .build();
        entity.setOrganizationId(organizationId);

        TdsDeducteeEntryEntity saved = tdsDeducteeEntryRepository.save(entity);

        // Update Challan utilization if linked
        if (request.getChallanId() != null) {
            tdsChallanRepository.findByIdAndOrganizationId(request.getChallanId(), organizationId).ifPresent(ch -> {
                BigDecimal newUtilized = ch.getUtilizedAmount().add(totalDeducted);
                ch.setUtilizedAmount(newUtilized);
                ch.setBalanceAmount(ch.getTotalAmount().subtract(newUtilized));
                if (ch.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    ch.setChallanStatus(ChallanStatus.FULLY_UTILIZED);
                } else {
                    ch.setChallanStatus(ChallanStatus.PARTIALLY_UTILIZED);
                }
                tdsChallanRepository.save(ch);
            });
        }

        return tdsMapper.toDeducteeDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsDeducteeEntryDto> getDeducteesByProfile(UUID tdsProfileId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return tdsDeducteeEntryRepository.findAllByOrganizationIdAndTdsProfileId(organizationId, tdsProfileId).stream()
                .map(tdsMapper::toDeducteeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsDeducteeEntryDto> getDeducteesByReturn(UUID tdsReturnId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return tdsDeducteeEntryRepository.findAllByOrganizationIdAndTdsReturnId(organizationId, tdsReturnId).stream()
                .map(tdsMapper::toDeducteeDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 5. Form 16 / 16A Certificates
    // =========================================================================

    @Override
    @Transactional
    public TdsCertificateDto createCertificate(CreateTdsCertificateRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        TdsProfileEntity profile = tdsProfileRepository.findByIdAndOrganizationId(request.getTdsProfileId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Profile", "id", request.getTdsProfileId()));

        TdsCertificateEntity entity = TdsCertificateEntity.builder()
                .tdsProfileId(request.getTdsProfileId())
                .tdsReturnId(request.getTdsReturnId())
                .certificateType(request.getCertificateType())
                .financialYear(request.getFinancialYear())
                .quarter(request.getQuarter())
                .deducteePan(request.getDeducteePan().toUpperCase().trim())
                .deducteeName(request.getDeducteeName())
                .tracesRequestNumber(request.getTracesRequestNumber())
                .certificateNumber(request.getCertificateNumber())
                .generationDate(request.getGenerationDate())
                .dispatchStatus(request.getDispatchStatus())
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        TdsCertificateEntity saved = tdsCertificateRepository.save(entity);
        return enrichCertificateEntity(saved, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsCertificateDto> getCertificatesByProfile(UUID tdsProfileId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return tdsCertificateRepository.findAllByOrganizationIdAndTdsProfileId(organizationId, tdsProfileId).stream()
                .map(this::enrichCertificateEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TdsCertificateDto updateCertificateStatus(UUID id, UpdateTdsCertificateStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        TdsCertificateEntity entity = tdsCertificateRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("TDS Certificate", "id", id));

        entity.setDispatchStatus(request.getDispatchStatus());
        if (request.getCertificateNumber() != null) entity.setCertificateNumber(request.getCertificateNumber());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());
        if (request.getDispatchStatus() == TdsCertificateEntity.DispatchStatus.SENT_TO_CLIENT || request.getDispatchStatus() == TdsCertificateEntity.DispatchStatus.SENT_TO_DEDUCTEE) {
            entity.setDispatchedAt(java.time.Instant.now());
        }

        TdsCertificateEntity updated = tdsCertificateRepository.save(entity);
        return enrichCertificateEntity(updated);
    }

    // =========================================================================
    // 6. Workload Dashboard & Calculator
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public TdsWorkloadDashboardDto getWorkloadDashboard(String quarterStr, String financialYear, UUID assignedEmployeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        String fy = StringUtils.hasText(financialYear) ? financialYear : "2026-27";
        TdsQuarter quarter = null;
        if (StringUtils.hasText(quarterStr) && !"ALL".equalsIgnoreCase(quarterStr)) {
            try {
                quarter = TdsQuarter.valueOf(quarterStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<TdsProfileEntity> profiles = tdsProfileRepository.findAllByOrganizationId(organizationId);
        int totalTans = profiles.size();
        int activeTans = (int) profiles.stream().filter(p -> p.getStatus() == TdsProfileEntity.TdsProfileStatus.ACTIVE).count();

        List<TdsReturnEntity> allReturns = tdsReturnRepository.findAllByOrganizationIdAndQuarterAndFinancialYear(
                organizationId, quarter != null ? quarter : TdsQuarter.Q1, fy
        );

        if (assignedEmployeeId != null) {
            allReturns = allReturns.stream().filter(r -> assignedEmployeeId.equals(r.getAssignedEmployeeId())).collect(Collectors.toList());
        }

        int totalScheduled = allReturns.size();
        int filed = (int) allReturns.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.FILED).count();
        int pending = (int) allReturns.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.PENDING || r.getFilingStatus() == TdsFilingStatus.DRAFT).count();
        int underReview = (int) allReturns.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.UNDER_REVIEW || r.getFilingStatus() == TdsFilingStatus.READY_TO_FILE).count();
        int overdue = (int) allReturns.stream().filter(r -> r.getFilingStatus() == TdsFilingStatus.OVERDUE || (r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now()) && r.getFilingStatus() != TdsFilingStatus.FILED)).count();

        BigDecimal totalTdsDeducted = allReturns.stream().map(TdsReturnEntity::getTotalTaxDeducted).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalChallanPaid = allReturns.stream().map(TdsReturnEntity::getTotalTaxDeposited).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TdsReturnDto> cards = allReturns.stream().map(this::enrichReturnEntity).collect(Collectors.toList());

        return TdsWorkloadDashboardDto.builder()
                .quarter(quarter != null ? quarter.name() : "Q1")
                .financialYear(fy)
                .totalTanClients(totalTans)
                .activeTanProfiles(activeTans)
                .totalScheduledReturns(totalScheduled)
                .filedReturns(filed)
                .pendingReturns(pending)
                .underReviewReturns(underReview)
                .overdueReturns(overdue)
                .totalPracticeTdsDeducted(totalTdsDeducted)
                .totalPracticeChallansPaid(totalChallanPaid)
                .unutilizedChallanBalance(BigDecimal.ZERO)
                .pendingCertificatesCount(0)
                .returnCards(cards)
                .build();
    }

    @Override
    public TdsComputationResultDto computeTds(TdsComputationRequest request) {
        return tdsCalculatorService.computeTds(request);
    }

    @Override
    public List<TdsSectionRateDto> getSectionRates() {
        return tdsCalculatorService.getAllSectionRates();
    }

    // =========================================================================
    // 7. Demo Data Seeder
    // =========================================================================

    @Override
    @Transactional
    public List<TdsReturnDto> seedDemoData() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<ClientEntity> clients = clientRepository.findAllByOrganizationId(organizationId);

        if (clients.isEmpty()) {
            return Collections.emptyList();
        }

        List<TdsReturnDto> seeded = new ArrayList<>();
        String[] sampleTans = {"BLRP12345A", "DELC98765B", "MUMP45678C", "HYDA23456D", "PUNC34567E"};
        int tanIdx = 0;

        for (ClientEntity client : clients) {
            if (tanIdx >= sampleTans.length) break;
            String tan = sampleTans[tanIdx++];

            Optional<TdsProfileEntity> existingProf = tdsProfileRepository.findByOrganizationIdAndTan(organizationId, tan);
            TdsProfileEntity profile;
            if (existingProf.isEmpty()) {
                profile = tdsProfileRepository.save(TdsProfileEntity.builder()
                        .clientId(client.getId())
                        .tan(tan)
                        .deductorType(TdsProfileEntity.DeductorType.COMPANY)
                        .responsiblePersonName(client.getContactPersonName() != null ? client.getContactPersonName() : "Director")
                        .responsiblePersonPan(client.getPan())
                        .responsiblePersonDesignation("Managing Director")
                        .responsiblePersonEmail(client.getEmail())
                        .responsiblePersonMobile(client.getPhone())
                        .status(TdsProfileEntity.TdsProfileStatus.ACTIVE)
                        .tracesStatus(TdsProfileEntity.TracesStatus.REGISTERED_ACTIVE)
                        .build());
                profile.setOrganizationId(organizationId);
                profile = tdsProfileRepository.save(profile);
            } else {
                profile = existingProf.get();
            }

            // Create Challan ITNS 281
            TdsChallanEntity challan = tdsChallanRepository.save(TdsChallanEntity.builder()
                    .tdsProfileId(profile.getId())
                    .bsrCode("0510304")
                    .challanDate(LocalDate.of(2026, 7, 6))
                    .challanSerialNo("001" + tanIdx)
                    .cin("051030420260706001" + tanIdx)
                    .majorHead(TdsChallanEntity.MajorHead.HEAD_0021_NON_COMPANY)
                    .minorHead(TdsChallanEntity.MinorHead.HEAD_200_PAYABLE_BY_TAXPAYER)
                    .sectionCode("194C")
                    .tdsAmount(new BigDecimal("45000.00"))
                    .totalAmount(new BigDecimal("45000.00"))
                    .utilizedAmount(new BigDecimal("45000.00"))
                    .balanceAmount(BigDecimal.ZERO)
                    .challanStatus(ChallanStatus.FULLY_UTILIZED)
                    .quarter(TdsQuarter.Q1)
                    .financialYear("2026-27")
                    .build());
            challan.setOrganizationId(organizationId);
            tdsChallanRepository.save(challan);

            // Create Return Form 26Q for Q1 FY 2026-27
            Optional<TdsReturnEntity> existingRet = tdsReturnRepository.findByOrganizationIdAndTdsProfileIdAndFormTypeAndQuarterAndFinancialYear(
                    organizationId, profile.getId(), TdsFormType.FORM_26Q, TdsQuarter.Q1, "2026-27"
            );

            if (existingRet.isEmpty()) {
                TdsReturnEntity ret = tdsReturnRepository.save(TdsReturnEntity.builder()
                        .clientId(client.getId())
                        .tdsProfileId(profile.getId())
                        .formType(TdsFormType.FORM_26Q)
                        .quarter(TdsQuarter.Q1)
                        .financialYear("2026-27")
                        .assessmentYear("2027-28")
                        .dueDate(LocalDate.of(2026, 7, 31))
                        .filingStatus(TdsFilingStatus.FILED)
                        .filingDate(LocalDate.of(2026, 7, 28))
                        .tokenNumber("01002030405060" + tanIdx)
                        .totalAmountPaid(new BigDecimal("2250000.00"))
                        .totalTaxDeducted(new BigDecimal("45000.00"))
                        .totalTaxDeposited(new BigDecimal("45000.00"))
                        .fvuValidationStatus(FvuValidationStatus.VALIDATED)
                        .notes("Filed on time with valid token acknowledgement.")
                        .build());
                ret.setOrganizationId(organizationId);
                ret = tdsReturnRepository.save(ret);
                seeded.add(enrichReturnEntity(ret));
            }
        }

        return seeded;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private LocalDate calculateQuarterlyDueDate(TdsQuarter quarter, String fy) {
        int startYear = Integer.parseInt(fy.split("-")[0]);
        return switch (quarter) {
            case Q1 -> LocalDate.of(startYear, 7, 31);
            case Q2 -> LocalDate.of(startYear, 10, 31);
            case Q3 -> LocalDate.of(startYear + 1, 1, 31);
            case Q4 -> LocalDate.of(startYear + 1, 5, 31);
        };
    }

    private String computeAssessmentYear(String financialYear) {
        if (!StringUtils.hasText(financialYear) || !financialYear.contains("-")) return "2027-28";
        String[] parts = financialYear.split("-");
        int start = Integer.parseInt(parts[0]) + 1;
        int end = Integer.parseInt(parts[1]) + 1;
        return start + "-" + (end < 10 ? "0" + end : String.valueOf(end));
    }

    private TdsProfileDto enrichProfileEntity(TdsProfileEntity entity) {
        TdsProfileDto dto = tdsMapper.toProfileDto(entity);
        ClientEntity client = clientRepository.findById(entity.getClientId()).orElse(null);
        EmployeeEntity emp = entity.getAssignedEmployeeId() != null ? employeeRepository.findById(entity.getAssignedEmployeeId()).orElse(null) : null;
        return enrichProfileDto(dto, client, emp);
    }

    private TdsProfileDto enrichProfileDto(TdsProfileDto dto, ClientEntity client, EmployeeEntity emp) {
        if (client != null) {
            dto.setClientName(client.getDisplayName() != null ? client.getDisplayName() : client.getLegalName());
        }
        if (emp != null) {
            dto.setAssignedEmployeeName(emp.getFullName());
        }
        return dto;
    }

    private TdsReturnDto enrichReturnEntity(TdsReturnEntity entity) {
        TdsReturnDto dto = tdsMapper.toReturnDto(entity);
        clientRepository.findById(entity.getClientId()).ifPresent(c -> {
            dto.setClientName(c.getDisplayName() != null ? c.getDisplayName() : c.getLegalName());
        });
        tdsProfileRepository.findById(entity.getTdsProfileId()).ifPresent(p -> {
            dto.setTan(p.getTan());
        });
        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findById(entity.getAssignedEmployeeId()).ifPresent(e -> {
                dto.setAssignedEmployeeName(e.getFullName());
            });
        }
        return dto;
    }

    private TdsChallanDto enrichChallanEntity(TdsChallanEntity entity) {
        TdsProfileEntity profile = tdsProfileRepository.findById(entity.getTdsProfileId()).orElse(null);
        return enrichChallanEntity(entity, profile);
    }

    private TdsChallanDto enrichChallanEntity(TdsChallanEntity entity, TdsProfileEntity profile) {
        TdsChallanDto dto = tdsMapper.toChallanDto(entity);
        if (profile != null) {
            dto.setTan(profile.getTan());
            clientRepository.findById(profile.getClientId()).ifPresent(c -> {
                dto.setClientName(c.getDisplayName() != null ? c.getDisplayName() : c.getLegalName());
            });
        }
        return dto;
    }

    private TdsCertificateDto enrichCertificateEntity(TdsCertificateEntity entity) {
        TdsProfileEntity profile = tdsProfileRepository.findById(entity.getTdsProfileId()).orElse(null);
        return enrichCertificateEntity(entity, profile);
    }

    private TdsCertificateDto enrichCertificateEntity(TdsCertificateEntity entity, TdsProfileEntity profile) {
        TdsCertificateDto dto = tdsMapper.toCertificateDto(entity);
        if (profile != null) {
            dto.setTan(profile.getTan());
            clientRepository.findById(profile.getClientId()).ifPresent(c -> {
                dto.setClientName(c.getDisplayName() != null ? c.getDisplayName() : c.getLegalName());
            });
        }
        return dto;
    }
}
