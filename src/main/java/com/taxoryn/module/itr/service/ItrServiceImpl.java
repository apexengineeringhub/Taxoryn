package com.taxoryn.module.itr.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.itr.dto.AssignItrEmployeeRequest;
import com.taxoryn.module.itr.dto.BatchGenerateItrReturnsRequest;
import com.taxoryn.module.itr.dto.BulkItrImportResultDto;
import com.taxoryn.module.itr.dto.CreateItrProfileRequest;
import com.taxoryn.module.itr.dto.CreateItrReturnRequest;
import com.taxoryn.module.itr.dto.ItrFilterRequest;
import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.dto.ItrWorkloadDashboardDto;
import com.taxoryn.module.itr.dto.ItrWorkloadDashboardDto.ItrClientWorkloadItem;
import com.taxoryn.module.itr.dto.RecordItrFilingRequest;
import com.taxoryn.module.itr.dto.UpdateItrProfileRequest;
import com.taxoryn.module.itr.dto.UpdateItrReturnRequest;
import com.taxoryn.module.itr.dto.UpdateItrStatusRequest;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.mapper.ItrMapper;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItrServiceImpl implements ItrService {

    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ItrMapper itrMapper;
    private final com.taxoryn.module.audit.service.AuditService auditService;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;

    // =========================================================================
    // 1. ITR Profile Management
    // =========================================================================

    @Override
    @Transactional
    public ItrProfileDto createProfile(CreateItrProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        String formattedPan = request.getPan().toUpperCase().trim();
        if (itrProfileRepository.existsByOrganizationIdAndPan(organizationId, formattedPan)) {
            throw new DuplicateResourceException("ITR Profile", "pan", formattedPan);
        }

        ClientEntity client = resolveOrCreateClient(request.getClientId(), formattedPan, request.getDisplayName(), request.getLegalName(), request.getTaxpayerType(), organizationId);

        if (itrProfileRepository.existsByOrganizationIdAndClientId(organizationId, client.getId())) {
            throw new DuplicateResourceException("ITR Profile", "clientId", client.getId().toString());
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        ItrProfileEntity profile = ItrProfileEntity.builder()
                .clientId(client.getId())
                .pan(formattedPan)
                .taxpayerType(request.getTaxpayerType() != null ? request.getTaxpayerType() : mapClientTypeToTaxpayerType(client.getClientType()))
                .defaultItrType(request.getDefaultItrType() != null ? request.getDefaultItrType() : ItrType.ITR_1)
                .residentialStatus(request.getResidentialStatus() != null ? request.getResidentialStatus() : ItrProfileEntity.ResidentialStatus.RESIDENT)
                .assignedEmployeeId(request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId() : client.getAssignedEmployeeId())
                .status(ItrProfileStatus.ACTIVE)
                .build();
        profile.setOrganizationId(organizationId);

        ItrProfileEntity saved = itrProfileRepository.save(profile);

        // Update client PAN if not set
        if (!StringUtils.hasText(client.getPan())) {
            client.setPan(formattedPan);
            clientRepository.save(client);
        }

        log.info("Created ITR Profile: id={}, pan={} for tenant={}", saved.getId(), saved.getPan(), organizationId);
        ItrProfileDto result = enrichProfileDto(saved);
        auditService.logEvent("ITR_PROFILE_CREATED", "ITR_PROFILE", saved.getId().toString(), null, result);
        return result;
    }

    @Override
    @Transactional
    public BulkItrImportResultDto bulkCreateProfiles(List<CreateItrProfileRequest> requests) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        BulkItrImportResultDto result = BulkItrImportResultDto.builder()
                .totalProcessed(requests != null ? requests.size() : 0)
                .build();

        if (requests == null || requests.isEmpty()) {
            return result;
        }

        int row = 1;
        for (CreateItrProfileRequest req : requests) {
            row++;
            try {
                String formattedPan = req.getPan() != null ? req.getPan().toUpperCase().trim() : null;
                if (formattedPan == null || formattedPan.isBlank()) {
                    result.getErrors().add("Row " + row + ": PAN is required");
                    result.setTotalFailed(result.getTotalFailed() + 1);
                    continue;
                }

                if (itrProfileRepository.existsByOrganizationIdAndPan(organizationId, formattedPan)) {
                    result.getErrors().add("Row " + row + " (PAN " + formattedPan + "): Already registered in practice, skipped");
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                ClientEntity client = resolveOrCreateClient(req.getClientId(), formattedPan, req.getDisplayName(), req.getLegalName(), req.getTaxpayerType(), organizationId);

                if (itrProfileRepository.existsByOrganizationIdAndClientId(organizationId, client.getId())) {
                    result.getErrors().add("Row " + row + " (Client " + client.getDisplayName() + "): ITR profile already registered, skipped");
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                TaxpayerType tType = req.getTaxpayerType() != null ? req.getTaxpayerType() : mapClientTypeToTaxpayerType(client.getClientType());
                ItrType iType = req.getDefaultItrType() != null ? req.getDefaultItrType() : ItrType.ITR_1;

                ItrProfileEntity profile = ItrProfileEntity.builder()
                        .clientId(client.getId())
                        .pan(formattedPan)
                        .taxpayerType(tType)
                        .defaultItrType(iType)
                        .residentialStatus(req.getResidentialStatus() != null ? req.getResidentialStatus() : ItrProfileEntity.ResidentialStatus.RESIDENT)
                        .assignedEmployeeId(req.getAssignedEmployeeId() != null ? req.getAssignedEmployeeId() : client.getAssignedEmployeeId())
                        .status(ItrProfileStatus.ACTIVE)
                        .build();
                profile.setOrganizationId(organizationId);

                ItrProfileEntity saved = itrProfileRepository.save(profile);

                if (!StringUtils.hasText(client.getPan())) {
                    client.setPan(formattedPan);
                    clientRepository.save(client);
                }

                result.getImportedItems().add(saved.getPan() + " (" + client.getDisplayName() + " - " + saved.getDefaultItrType() + ")");
                result.setTotalCreated(result.getTotalCreated() + 1);

                auditService.logEvent("ITR_PROFILE_BULK_IMPORTED", "ITR_PROFILE", saved.getId().toString(), null, saved.getPan());
            } catch (Exception ex) {
                result.getErrors().add("Row " + row + " (PAN " + req.getPan() + "): " + ex.getMessage());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        log.info("Bulk imported ITR profiles for orgId={}: {} created, {} skipped, {} failed",
                organizationId, result.getTotalCreated(), result.getTotalSkipped(), result.getTotalFailed());

        return result;
    }

    @Override
    @Transactional
    public ItrProfileDto updateProfile(UUID id, UpdateItrProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrProfileEntity profile = itrProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Profile", "id", id));

        ItrProfileDto oldSnapshot = enrichProfileDto(profile);

        String newPan = request.getPan().toUpperCase().trim();
        if (!newPan.equalsIgnoreCase(profile.getPan())
                && itrProfileRepository.existsByOrganizationIdAndPan(organizationId, newPan)) {
            throw new DuplicateResourceException("ITR Profile", "pan", newPan);
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        profile.setPan(newPan);
        profile.setTaxpayerType(request.getTaxpayerType());
        profile.setDefaultItrType(request.getDefaultItrType());
        if (request.getResidentialStatus() != null) {
            profile.setResidentialStatus(request.getResidentialStatus());
        }
        profile.setAssignedEmployeeId(request.getAssignedEmployeeId());
        if (request.getStatus() != null) {
            profile.setStatus(request.getStatus());
        }

        ItrProfileEntity saved = itrProfileRepository.save(profile);
        log.info("Updated ITR Profile: id={} for tenant={}", saved.getId(), organizationId);
        ItrProfileDto result = enrichProfileDto(saved);
        auditService.logEvent("ITR_PROFILE_UPDATED", "ITR_PROFILE", saved.getId().toString(), oldSnapshot, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ItrProfileDto getProfileById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrProfileEntity profile = itrProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Profile", "id", id));

        return enrichProfileDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ItrProfileDto getProfileByClientId(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrProfileEntity profile = itrProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Profile for Client", "clientId", clientId));

        return enrichProfileDto(profile);
    }

    // =========================================================================
    // 2. ITR Returns Lifecycle
    // =========================================================================

    @Override
    @Transactional
    public ItrReturnDto createReturn(CreateItrReturnRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId).orElse(null);
        }
        if (client == null && StringUtils.hasText(request.getPan())) {
            client = resolveOrCreateClient(null, request.getPan(), null, null, request.getTaxpayerType(), organizationId);
        }

        if (client == null) {
            throw new ResourceNotFoundException("Client", "clientId/pan", request.getClientId() != null ? request.getClientId() : request.getPan());
        }

        String formattedAy = request.getAssessmentYear().trim();
        if (itrReturnRepository.existsByOrganizationIdAndClientIdAndAssessmentYear(organizationId, client.getId(), formattedAy)) {
            throw new DuplicateResourceException("ITR Return", "assessmentYear", formattedAy + " for Client " + client.getDisplayName());
        }

        Optional<ItrProfileEntity> profileOpt = itrProfileRepository.findByOrganizationIdAndClientId(organizationId, client.getId());
        UUID profileId = profileOpt.map(ItrProfileEntity::getId).orElse(null);

        TaxpayerType taxpayerType = request.getTaxpayerType();
        if (taxpayerType == null) {
            if (profileOpt.isPresent()) {
                taxpayerType = profileOpt.get().getTaxpayerType();
            } else {
                taxpayerType = mapClientTypeToTaxpayerType(client.getClientType());
            }
        }

        UUID assignedEmpId = request.getAssignedEmployeeId();
        if (assignedEmpId == null) {
            assignedEmpId = profileOpt.map(ItrProfileEntity::getAssignedEmployeeId).orElse(client.getAssignedEmployeeId());
        }

        LocalDate dueDate = request.getDueDate();
        if (dueDate == null) {
            dueDate = deriveDefaultDueDate(formattedAy, taxpayerType);
        }

        ItrStatus status = request.getStatus() != null ? request.getStatus() : (StringUtils.hasText(request.getAcknowledgementNumber()) ? ItrStatus.FILED : ItrStatus.DOCUMENTS_PENDING);

        ItrReturnEntity entity = ItrReturnEntity.builder()
                .clientId(client.getId())
                .itrProfileId(profileId)
                .assessmentYear(formattedAy)
                .financialYear(request.getFinancialYear().trim())
                .itrType(request.getItrType() != null ? request.getItrType() : ItrType.ITR_1)
                .taxpayerType(taxpayerType)
                .dueDate(dueDate)
                .filingDate(request.getFilingDate() != null ? request.getFilingDate() : (status == ItrStatus.FILED ? LocalDate.now() : null))
                .acknowledgementNumber(request.getAcknowledgementNumber())
                .status(status)
                .assignedEmployeeId(assignedEmpId)
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        ItrReturnEntity saved = itrReturnRepository.save(entity);
        log.info("Created ITR return: id={}, client={}, AY={} for tenant={}", saved.getId(), client.getDisplayName(), saved.getAssessmentYear(), organizationId);
        ItrReturnDto result = enrichReturnDto(saved);
        auditService.logEvent("ITR_RETURN_CREATED", "ITR_RETURN", saved.getId().toString(), null, result);
        return result;
    }

    @Override
    @Transactional
    public BulkItrImportResultDto bulkCreateReturns(List<CreateItrReturnRequest> requests) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        BulkItrImportResultDto result = BulkItrImportResultDto.builder()
                .totalProcessed(requests != null ? requests.size() : 0)
                .build();

        if (requests == null || requests.isEmpty()) {
            return result;
        }

        int row = 1;
        for (CreateItrReturnRequest req : requests) {
            row++;
            try {
                ClientEntity client = null;
                if (req.getClientId() != null) {
                    client = clientRepository.findByIdAndOrganizationId(req.getClientId(), organizationId).orElse(null);
                }
                if (client == null && StringUtils.hasText(req.getPan())) {
                    client = clientRepository.findByOrganizationIdAndPan(organizationId, req.getPan().toUpperCase().trim()).orElse(null);
                    if (client == null) {
                        client = resolveOrCreateClient(null, req.getPan(), null, null, req.getTaxpayerType(), organizationId);
                    }
                }

                if (client == null) {
                    result.getErrors().add("Row " + row + ": Client PAN or ID not resolved");
                    result.setTotalFailed(result.getTotalFailed() + 1);
                    continue;
                }

                String ay = req.getAssessmentYear() != null ? req.getAssessmentYear().trim() : "2026-27";
                String fy = req.getFinancialYear() != null ? req.getFinancialYear().trim() : "2025-26";

                if (itrReturnRepository.existsByOrganizationIdAndClientIdAndAssessmentYear(organizationId, client.getId(), ay)) {
                    result.getErrors().add("Row " + row + " (" + client.getDisplayName() + " AY " + ay + "): Return record already exists, skipped");
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                Optional<ItrProfileEntity> profileOpt = itrProfileRepository.findByOrganizationIdAndClientId(organizationId, client.getId());
                UUID profileId = profileOpt.map(ItrProfileEntity::getId).orElse(null);
                if (profileId == null) {
                    ItrProfileEntity newProfile = ItrProfileEntity.builder()
                            .clientId(client.getId())
                            .pan(client.getPan() != null ? client.getPan() : (req.getPan() != null ? req.getPan().toUpperCase().trim() : "UNKNOWN"))
                            .taxpayerType(req.getTaxpayerType() != null ? req.getTaxpayerType() : mapClientTypeToTaxpayerType(client.getClientType()))
                            .defaultItrType(req.getItrType() != null ? req.getItrType() : ItrType.ITR_1)
                            .status(ItrProfileStatus.ACTIVE)
                            .build();
                    newProfile.setOrganizationId(organizationId);
                    ItrProfileEntity savedProfile = itrProfileRepository.save(newProfile);
                    profileId = savedProfile.getId();
                }

                TaxpayerType tType = req.getTaxpayerType() != null
                        ? req.getTaxpayerType()
                        : (profileOpt.map(ItrProfileEntity::getTaxpayerType).orElse(mapClientTypeToTaxpayerType(client.getClientType())));

                ItrType iType = req.getItrType() != null
                        ? req.getItrType()
                        : (profileOpt.map(ItrProfileEntity::getDefaultItrType).orElse(ItrType.ITR_1));

                LocalDate dueDate = req.getDueDate() != null ? req.getDueDate() : deriveDefaultDueDate(ay, tType);

                ItrStatus status = req.getStatus() != null ? req.getStatus() : (StringUtils.hasText(req.getAcknowledgementNumber()) ? ItrStatus.FILED : ItrStatus.DOCUMENTS_PENDING);

                ItrReturnEntity entity = ItrReturnEntity.builder()
                        .clientId(client.getId())
                        .itrProfileId(profileId)
                        .assessmentYear(ay)
                        .financialYear(fy)
                        .itrType(iType)
                        .taxpayerType(tType)
                        .dueDate(dueDate)
                        .filingDate(req.getFilingDate() != null ? req.getFilingDate() : (status == ItrStatus.FILED ? LocalDate.now() : null))
                        .acknowledgementNumber(req.getAcknowledgementNumber())
                        .status(status)
                        .assignedEmployeeId(req.getAssignedEmployeeId() != null ? req.getAssignedEmployeeId() : client.getAssignedEmployeeId())
                        .notes(req.getNotes())
                        .build();
                entity.setOrganizationId(organizationId);

                ItrReturnEntity saved = itrReturnRepository.save(entity);
                result.getImportedItems().add(client.getDisplayName() + " (AY " + saved.getAssessmentYear() + " - " + saved.getItrType() + ")");
                result.setTotalCreated(result.getTotalCreated() + 1);

                auditService.logEvent("ITR_RETURN_BULK_IMPORTED", "ITR_RETURN", saved.getId().toString(), null, saved.getAssessmentYear());
            } catch (Exception ex) {
                result.getErrors().add("Row " + row + ": " + ex.getMessage());
                result.setTotalFailed(result.getTotalFailed() + 1);
            }
        }

        log.info("Bulk imported ITR returns for orgId={}: {} created, {} skipped, {} failed",
                organizationId, result.getTotalCreated(), result.getTotalSkipped(), result.getTotalFailed());

        return result;
    }

    @Override
    @Transactional
    public List<ItrReturnDto> batchGenerateReturns(BatchGenerateItrReturnsRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<ItrProfileEntity> activeProfiles = new ArrayList<>(itrProfileRepository.findAllByOrganizationIdAndStatus(organizationId, ItrProfileStatus.ACTIVE));

        // If no ITR profiles exist yet, auto-discover all practice clients and initialize their ITR profiles
        if (activeProfiles.isEmpty()) {
            List<ClientEntity> allClients = clientRepository.findAllByOrganizationId(organizationId);
            for (ClientEntity client : allClients) {
                String pan = StringUtils.hasText(client.getPan()) ? client.getPan().toUpperCase().trim() : ("PAN" + client.getId().toString().substring(0, 7).toUpperCase());
                TaxpayerType tType = mapClientTypeToTaxpayerType(client.getClientType());
                ItrType defForm = switch (tType) {
                    case COMPANY -> ItrType.ITR_6;
                    case LLP, FIRM -> ItrType.ITR_5;
                    case TRUST -> ItrType.ITR_7;
                    default -> ItrType.ITR_1;
                };

                ItrProfileEntity newProfile = ItrProfileEntity.builder()
                        .clientId(client.getId())
                        .pan(pan)
                        .taxpayerType(tType)
                        .defaultItrType(defForm)
                        .residentialStatus(ItrProfileEntity.ResidentialStatus.RESIDENT)
                        .assignedEmployeeId(client.getAssignedEmployeeId())
                        .status(ItrProfileStatus.ACTIVE)
                        .build();
                newProfile.setOrganizationId(organizationId);
                ItrProfileEntity saved = itrProfileRepository.save(newProfile);
                activeProfiles.add(saved);
            }
        }

        List<ItrReturnDto> createdReturns = new ArrayList<>();
        String ay = request.getAssessmentYear().trim();
        String fy = request.getFinancialYear().trim();

        for (ItrProfileEntity profile : activeProfiles) {
            ItrType formType = profile.getDefaultItrType();
            if (request.getItrTypes() != null && !request.getItrTypes().isEmpty() && !request.getItrTypes().contains(formType)) {
                continue;
            }

            if (!itrReturnRepository.existsByOrganizationIdAndClientIdAndAssessmentYear(organizationId, profile.getClientId(), ay)) {
                LocalDate dueDate;
                if (profile.getTaxpayerType() == TaxpayerType.COMPANY || profile.getTaxpayerType() == TaxpayerType.LLP || formType == ItrType.ITR_6) {
                    dueDate = request.getAuditDueDate() != null ? request.getAuditDueDate() : deriveDefaultDueDate(ay, profile.getTaxpayerType());
                } else {
                    dueDate = request.getNonAuditDueDate() != null ? request.getNonAuditDueDate() : deriveDefaultDueDate(ay, profile.getTaxpayerType());
                }

                ItrReturnEntity entity = ItrReturnEntity.builder()
                        .clientId(profile.getClientId())
                        .itrProfileId(profile.getId())
                        .assessmentYear(ay)
                        .financialYear(fy)
                        .itrType(formType)
                        .taxpayerType(profile.getTaxpayerType())
                        .dueDate(dueDate)
                        .status(ItrStatus.DOCUMENTS_PENDING)
                        .assignedEmployeeId(profile.getAssignedEmployeeId())
                        .build();
                entity.setOrganizationId(organizationId);

                ItrReturnEntity saved = itrReturnRepository.save(entity);
                createdReturns.add(enrichReturnDto(saved));
            }
        }

        log.info("Batch generated {} ITR returns for AY {} in tenant {}", createdReturns.size(), ay, organizationId);
        auditService.logEvent("ITR_BATCH_GENERATED", "ITR_RETURN", ay, null, "Generated " + createdReturns.size() + " returns");
        return createdReturns;
    }

    @Override
    @Transactional
    public ItrReturnDto updateReturn(UUID id, UpdateItrReturnRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity entity = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        ItrReturnDto oldSnapshot = enrichReturnDto(entity);

        if (request.getItrType() != null) entity.setItrType(request.getItrType());
        if (request.getTaxpayerType() != null) entity.setTaxpayerType(request.getTaxpayerType());
        if (request.getDueDate() != null) entity.setDueDate(request.getDueDate());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
            entity.setAssignedEmployeeId(request.getAssignedEmployeeId());
        }
        if (request.getNotes() != null) entity.setNotes(request.getNotes());

        ItrReturnEntity saved = itrReturnRepository.save(entity);
        log.info("Updated ITR return: id={} for tenant={}", saved.getId(), organizationId);
        ItrReturnDto result = enrichReturnDto(saved);
        auditService.logEvent("ITR_RETURN_UPDATED", "ITR_RETURN", saved.getId().toString(), oldSnapshot, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ItrReturnDto getReturnById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity entity = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        return enrichReturnDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ItrReturnDto> getReturns(ItrFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<ItrReturnEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (!scope.isFirmAdmin()) {
                Set<UUID> accessibleClientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
                if (accessibleClientIds == null || accessibleClientIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("clientId").in(accessibleClientIds));
                }
            }

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }
            if (StringUtils.hasText(filterRequest.getAssessmentYear())) {
                predicates.add(cb.equal(root.get("assessmentYear"), filterRequest.getAssessmentYear().trim()));
            }
            if (StringUtils.hasText(filterRequest.getFinancialYear())) {
                predicates.add(cb.equal(root.get("financialYear"), filterRequest.getFinancialYear().trim()));
            }
            if (filterRequest.getItrType() != null) {
                predicates.add(cb.equal(root.get("itrType"), filterRequest.getItrType()));
            }
            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }
            if (filterRequest.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filterRequest.getAssignedEmployeeId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ItrReturnEntity> page = itrReturnRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::enrichReturnDto);
    }

    @Override
    @Transactional
    public ItrReturnDto updateStatus(UUID id, UpdateItrStatusRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity entity = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        ItrStatus oldStatus = entity.getStatus();
        entity.setStatus(request.getStatus());

        if (request.getStatus() == ItrStatus.FILED && entity.getFilingDate() == null) {
            entity.setFilingDate(LocalDate.now());
        }
        if (request.getStatus() == ItrStatus.COMPLETED && entity.getVerificationDate() == null) {
            entity.setVerificationDate(LocalDate.now());
        }

        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        ItrReturnEntity saved = itrReturnRepository.save(entity);
        log.info("Updated ITR return status: id={}, status={} for tenant={}", saved.getId(), saved.getStatus(), organizationId);
        ItrReturnDto result = enrichReturnDto(saved);
        auditService.logEvent("ITR_STATUS_UPDATED", "ITR_RETURN", saved.getId().toString(), oldStatus.name(), saved.getStatus().name());
        return result;
    }

    @Override
    @Transactional
    public ItrReturnDto recordFilingDetails(UUID id, RecordItrFilingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity entity = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        ItrReturnDto oldSnapshot = enrichReturnDto(entity);

        entity.setFilingDate(request.getFilingDate());
        entity.setAcknowledgementNumber(request.getAcknowledgementNumber().trim().toUpperCase());
        entity.setStatus(ItrStatus.FILED);

        if (request.getVerificationDate() != null) {
            entity.setVerificationDate(request.getVerificationDate());
            entity.setStatus(ItrStatus.COMPLETED);
        }

        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        ItrReturnEntity saved = itrReturnRepository.save(entity);
        log.info("Recorded ITR filing details: id={}, ackNo={} for tenant={}", saved.getId(), saved.getAcknowledgementNumber(), organizationId);
        ItrReturnDto result = enrichReturnDto(saved);
        auditService.logEvent("ITR_FILING_RECORDED", "ITR_RETURN", saved.getId().toString(), oldSnapshot, result);
        return result;
    }

    @Override
    @Transactional
    public ItrReturnDto assignEmployee(UUID id, AssignItrEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity entity = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        employeeRepository.findByIdAndOrganizationId(request.getEmployeeId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        entity.setAssignedEmployeeId(request.getEmployeeId());
        ItrReturnEntity saved = itrReturnRepository.save(entity);
        log.info("Assigned employee {} to ITR return {} for tenant {}", request.getEmployeeId(), id, organizationId);
        return enrichReturnDto(saved);
    }

    // =========================================================================
    // 3. Upcoming, Overdue, History & Workload
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ItrReturnDto> getUpcomingReturns(int daysAhead) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(daysAhead);

        List<ItrStatus> excluded = List.of(ItrStatus.FILED, ItrStatus.COMPLETED, ItrStatus.CANCELLED);
        List<ItrReturnEntity> list = itrReturnRepository.findAllByOrganizationIdAndDueDateBetweenAndStatusNotIn(
                organizationId, today, cutoff, excluded);

        return list.stream().map(this::enrichReturnDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItrReturnDto> getOverdueReturns() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();

        List<ItrStatus> excluded = List.of(ItrStatus.FILED, ItrStatus.COMPLETED, ItrStatus.CANCELLED);
        List<ItrReturnEntity> list = itrReturnRepository.findAllByOrganizationIdAndDueDateBetweenAndStatusNotIn(
                organizationId, LocalDate.of(2000, 1, 1), today.minusDays(1), excluded);

        return list.stream().map(this::enrichReturnDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItrReturnDto> getClientItrHistory(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<ItrReturnEntity> list = itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(organizationId, clientId);
        return list.stream().map(this::enrichReturnDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ItrWorkloadDashboardDto getWorkloadDashboard(String assessmentYear, UUID assignedEmployeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        String targetAy = StringUtils.hasText(assessmentYear) ? assessmentYear.trim() : deriveCurrentAssessmentYear();

        List<ItrReturnEntity> returns = itrReturnRepository.findAllByOrganizationIdAndAssessmentYear(organizationId, targetAy);
        if (assignedEmployeeId != null) {
            returns = returns.stream().filter(r -> assignedEmployeeId.equals(r.getAssignedEmployeeId())).toList();
        }

        long totalReturns = returns.size();
        long docPending = returns.stream().filter(r -> r.getStatus() == ItrStatus.DOCUMENTS_PENDING).count();
        long dataEntry = returns.stream().filter(r -> r.getStatus() == ItrStatus.DATA_ENTRY).count();
        long underReview = returns.stream().filter(r -> r.getStatus() == ItrStatus.UNDER_REVIEW).count();
        long readyToFile = returns.stream().filter(r -> r.getStatus() == ItrStatus.READY_TO_FILE).count();
        long filed = returns.stream().filter(r -> r.getStatus() == ItrStatus.FILED).count();
        long verified = returns.stream().filter(r -> r.getStatus() == ItrStatus.COMPLETED).count();

        LocalDate today = LocalDate.now();
        List<ItrStatus> unfiled = List.of(ItrStatus.DOCUMENTS_PENDING, ItrStatus.DATA_ENTRY, ItrStatus.UNDER_REVIEW, ItrStatus.READY_TO_FILE);
        long overdueCount = returns.stream().filter(r -> unfiled.contains(r.getStatus()) && r.getDueDate() != null && r.getDueDate().isBefore(today)).count();
        long upcomingCount = returns.stream().filter(r -> unfiled.contains(r.getStatus()) && r.getDueDate() != null && !r.getDueDate().isBefore(today) && r.getDueDate().isBefore(today.plusDays(30))).count();

        List<ItrClientWorkloadItem> clientItems = returns.stream().map(r -> {
            ClientEntity client = clientRepository.findByIdAndOrganizationId(r.getClientId(), organizationId).orElse(null);
            String employeeName = null;
            if (r.getAssignedEmployeeId() != null) {
                employeeName = employeeRepository.findByIdAndOrganizationId(r.getAssignedEmployeeId(), organizationId)
                        .map(EmployeeEntity::getFullName).orElse(null);
            }
            return ItrClientWorkloadItem.builder()
                    .returnId(r.getId())
                    .clientId(r.getClientId())
                    .clientName(client != null ? client.getDisplayName() : "Unknown")
                    .pan(client != null ? client.getPan() : null)
                    .itrType(r.getItrType())
                    .taxpayerType(r.getTaxpayerType())
                    .status(r.getStatus())
                    .dueDate(r.getDueDate())
                    .filingDate(r.getFilingDate())
                    .acknowledgementNumber(r.getAcknowledgementNumber())
                    .assignedTo(employeeName)
                    .build();
        }).toList();

        return ItrWorkloadDashboardDto.builder()
                .assessmentYear(targetAy)
                .totalReturns(totalReturns)
                .totalClients(totalReturns)
                .documentsPendingCount(docPending)
                .dataEntryCount(dataEntry)
                .underReviewCount(underReview)
                .readyToFileCount(readyToFile)
                .filedCount(filed)
                .completedCount(verified)
                .overdueCount(overdueCount)
                .upcomingCount(upcomingCount)
                .returns(clientItems)
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ClientEntity resolveOrCreateClient(UUID clientId, String pan, String displayName, String legalName, TaxpayerType taxpayerType, UUID organizationId) {
        if (clientId != null) {
            Optional<ClientEntity> clientOpt = clientRepository.findByIdAndOrganizationId(clientId, organizationId);
            if (clientOpt.isPresent()) {
                return clientOpt.get();
            }
        }

        String formattedPan = StringUtils.hasText(pan) ? pan.toUpperCase().trim() : null;
        if (formattedPan != null) {
            Optional<ClientEntity> clientOpt = clientRepository.findByOrganizationIdAndPan(organizationId, formattedPan);
            if (clientOpt.isPresent()) {
                return clientOpt.get();
            }
        }

        // Auto-create Client Entity for the Practice Tenant
        String dispName = StringUtils.hasText(displayName)
                ? displayName.trim()
                : (StringUtils.hasText(legalName) ? legalName.trim() : (formattedPan != null ? "Taxpayer " + formattedPan : "ITR Client"));

        ClientEntity.ClientType cType = ClientEntity.ClientType.INDIVIDUAL;
        if (taxpayerType != null) {
            switch (taxpayerType) {
                case COMPANY -> cType = ClientEntity.ClientType.PRIVATE_LIMITED;
                case LLP -> cType = ClientEntity.ClientType.LLP;
                case FIRM -> cType = ClientEntity.ClientType.PARTNERSHIP;
                case HUF -> cType = ClientEntity.ClientType.INDIVIDUAL;
                case TRUST -> cType = ClientEntity.ClientType.TRUST;
                case AOP_BOI -> cType = ClientEntity.ClientType.SOCIETY;
                default -> cType = ClientEntity.ClientType.INDIVIDUAL;
            }
        }

        ClientEntity newClient = ClientEntity.builder()
                .displayName(dispName)
                .legalName(StringUtils.hasText(legalName) ? legalName.trim() : dispName)
                .pan(formattedPan)
                .clientType(cType)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        newClient.setOrganizationId(organizationId);
        ClientEntity saved = clientRepository.save(newClient);
        log.info("Auto-created client entity during ITR onboarding for tenant {}: id={}, displayName={}, PAN={}", organizationId, saved.getId(), saved.getDisplayName(), formattedPan);
        return saved;
    }

    private ItrProfileDto enrichProfileDto(ItrProfileEntity profile) {
        if (profile == null) return null;
        ItrProfileDto dto = itrMapper.toProfileDto(profile);
        if (dto == null) return null;
        clientRepository.findByIdAndOrganizationId(profile.getClientId(), profile.getOrganizationId())
                .ifPresent(c -> dto.setClientName(c.getDisplayName()));

        if (profile.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(profile.getAssignedEmployeeId(), profile.getOrganizationId())
                    .ifPresent(e -> dto.setAssignedEmployeeName(e.getFullName()));
        }
        return dto;
    }

    private ItrReturnDto enrichReturnDto(ItrReturnEntity entity) {
        if (entity == null) return null;
        ItrReturnDto dto = itrMapper.toReturnDto(entity);
        if (dto == null) return null;
        clientRepository.findByIdAndOrganizationId(entity.getClientId(), entity.getOrganizationId())
                .ifPresent(c -> {
                    dto.setClientName(c.getDisplayName());
                    dto.setPan(c.getPan());
                });

        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(entity.getAssignedEmployeeId(), entity.getOrganizationId())
                    .ifPresent(e -> dto.setAssignedEmployeeName(e.getFullName()));
        }
        return dto;
    }

    private TaxpayerType mapClientTypeToTaxpayerType(ClientEntity.ClientType clientType) {
        if (clientType == null) return TaxpayerType.INDIVIDUAL;
        return switch (clientType) {
            case INDIVIDUAL, PROPRIETORSHIP -> TaxpayerType.INDIVIDUAL;
            case PARTNERSHIP -> TaxpayerType.FIRM;
            case LLP -> TaxpayerType.LLP;
            case PRIVATE_LIMITED, PUBLIC_LIMITED -> TaxpayerType.COMPANY;
            case TRUST -> TaxpayerType.TRUST;
            case SOCIETY -> TaxpayerType.AOP_BOI;
            default -> TaxpayerType.INDIVIDUAL;
        };
    }

    private LocalDate deriveDefaultDueDate(String assessmentYear, TaxpayerType taxpayerType) {
        try {
            int ayStartYear = Integer.parseInt(assessmentYear.substring(0, 4));
            if (taxpayerType == TaxpayerType.COMPANY || taxpayerType == TaxpayerType.LLP) {
                return LocalDate.of(ayStartYear, 10, 31); // October 31 for corporate / tax audit
            }
            return LocalDate.of(ayStartYear, 7, 31); // July 31 for non-audit individual / HUF / firm
        } catch (Exception e) {
            return LocalDate.now().plusMonths(3);
        }
    }

    private String deriveCurrentAssessmentYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int ayStart = month >= 4 ? year : year - 1;
        int ayEndShort = (ayStart + 1) % 100;
        return ayStart + "-" + String.format("%02d", ayEndShort);
    }

    @Override
    @Transactional
    public List<ItrReturnDto> seedDemoData() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        record DemoClient(String pan, String displayName, String legalName, ClientEntity.ClientType clientType,
                          TaxpayerType taxpayerType, ItrType defaultItrType, String email, String phone) {}

        List<DemoClient> demoClients = List.of(
                new DemoClient("ABCDE1234F", "Pawan Pathak & Associates", "Pawan Pathak & Associates", ClientEntity.ClientType.PARTNERSHIP, TaxpayerType.FIRM, ItrType.ITR_5, "pawan.tax@example.com", "9820112233"),
                new DemoClient("AABFA1234F", "MAA MUNDESHWARI ENTERPRISES", "MAA MUNDESHWARI ENTERPRISES PVT LTD", ClientEntity.ClientType.PRIVATE_LIMITED, TaxpayerType.COMPANY, ItrType.ITR_6, "mundeshwari.ent@example.com", "9833445566"),
                new DemoClient("BNZPS8821M", "Dr. Rajesh Sharma", "Dr. Rajesh Sharma", ClientEntity.ClientType.INDIVIDUAL, TaxpayerType.INDIVIDUAL, ItrType.ITR_1, "rajesh.sharma@example.com", "9811223344"),
                new DemoClient("CLXPT4412K", "Sneha Kulkarni", "Sneha Kulkarni", ClientEntity.ClientType.INDIVIDUAL, TaxpayerType.INDIVIDUAL, ItrType.ITR_2, "sneha.k@example.com", "9822334455"),
                new DemoClient("DKRPJ9931L", "Vikram Mehta (Consulting)", "Vikram Mehta", ClientEntity.ClientType.PROPRIETORSHIP, TaxpayerType.INDIVIDUAL, ItrType.ITR_3, "vikram.mehta@example.com", "9833445577"),
                new DemoClient("ELMPR3321Q", "Rohan Deshmukh (Retailer)", "Rohan Deshmukh", ClientEntity.ClientType.PROPRIETORSHIP, TaxpayerType.INDIVIDUAL, ItrType.ITR_4, "rohan.retail@example.com", "9844556677"),
                new DemoClient("FGKPA7712N", "Aarav Gupta HUF", "Aarav Gupta HUF", ClientEntity.ClientType.INDIVIDUAL, TaxpayerType.HUF, ItrType.ITR_2, "aarav.huf@example.com", "9855667788"),
                new DemoClient("AAATR5566D", "Shri Mundeshwari Seva Trust", "Shri Mundeshwari Seva Trust", ClientEntity.ClientType.TRUST, TaxpayerType.TRUST, ItrType.ITR_7, "trust.seva@example.com", "9866778899")
        );

        List<ItrReturnDto> results = new ArrayList<>();
        for (DemoClient d : demoClients) {
            ClientEntity client = clientRepository.findByOrganizationIdAndPan(organizationId, d.pan())
                    .orElseGet(() -> {
                        ClientEntity c = ClientEntity.builder()
                                .displayName(d.displayName())
                                .legalName(d.legalName())
                                .pan(d.pan())
                                .clientType(d.clientType())
                                .email(d.email())
                                .phone(d.phone())
                                .status(ClientEntity.ClientStatus.ACTIVE)
                                .build();
                        c.setOrganizationId(organizationId);
                        return clientRepository.save(c);
                    });

            ItrProfileEntity profile = itrProfileRepository.findByOrganizationIdAndClientId(organizationId, client.getId())
                    .orElseGet(() -> {
                        ItrProfileEntity p = ItrProfileEntity.builder()
                                .clientId(client.getId())
                                .pan(d.pan())
                                .taxpayerType(d.taxpayerType())
                                .defaultItrType(d.defaultItrType())
                                .residentialStatus(ItrProfileEntity.ResidentialStatus.RESIDENT)
                                .status(ItrProfileStatus.ACTIVE)
                                .build();
                        p.setOrganizationId(organizationId);
                        return itrProfileRepository.save(p);
                    });

            Optional<ItrReturnEntity> retOpt = itrReturnRepository.findByOrganizationIdAndClientIdAndAssessmentYear(organizationId, client.getId(), "2026-27");
            if (retOpt.isEmpty()) {
                LocalDate dueDate = (d.taxpayerType() == TaxpayerType.COMPANY || d.defaultItrType() == ItrType.ITR_6) ? LocalDate.of(2026, 10, 31) : LocalDate.of(2026, 7, 31);
                boolean isFiled = d.defaultItrType() == ItrType.ITR_1 || d.defaultItrType() == ItrType.ITR_6;
                ItrReturnEntity entity = ItrReturnEntity.builder()
                        .clientId(client.getId())
                        .itrProfileId(profile.getId())
                        .assessmentYear("2026-27")
                        .financialYear("2025-26")
                        .itrType(d.defaultItrType())
                        .taxpayerType(d.taxpayerType())
                        .dueDate(dueDate)
                        .filingDate(isFiled ? LocalDate.of(2026, 7, 28) : null)
                        .acknowledgementNumber(isFiled ? "109823487123984" : null)
                        .status(isFiled ? ItrStatus.FILED : ItrStatus.DOCUMENTS_PENDING)
                        .build();
                entity.setOrganizationId(organizationId);
                ItrReturnEntity saved = itrReturnRepository.save(entity);
                results.add(enrichReturnDto(saved));
            } else {
                results.add(enrichReturnDto(retOpt.get()));
            }
        }
        return results;
    }
}
