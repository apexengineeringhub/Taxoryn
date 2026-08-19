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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItrServiceImpl implements ItrService {

    private final ItrProfileRepository itrProfileRepository;
    private final ItrReturnRepository itrReturnRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ItrMapper itrMapper;

    // =========================================================================
    // 1. ITR Profile Management
    // =========================================================================

    @Override
    @Transactional
    public ItrProfileDto createProfile(CreateItrProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        String formattedPan = request.getPan().toUpperCase().trim();
        if (itrProfileRepository.existsByOrganizationIdAndPan(organizationId, formattedPan)) {
            throw new DuplicateResourceException("ITR Profile", "pan", formattedPan);
        }

        if (itrProfileRepository.existsByOrganizationIdAndClientId(organizationId, request.getClientId())) {
            throw new DuplicateResourceException("ITR Profile", "clientId", request.getClientId().toString());
        }

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
        }

        ItrProfileEntity profile = ItrProfileEntity.builder()
                .clientId(request.getClientId())
                .pan(formattedPan)
                .taxpayerType(request.getTaxpayerType())
                .defaultItrType(request.getDefaultItrType())
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
        return enrichProfileDto(saved);
    }

    @Override
    @Transactional
    public ItrProfileDto updateProfile(UUID id, UpdateItrProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrProfileEntity profile = itrProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Profile", "id", id));

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
        return enrichProfileDto(saved);
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

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        String formattedAy = request.getAssessmentYear().trim();
        if (itrReturnRepository.existsByOrganizationIdAndClientIdAndAssessmentYear(organizationId, request.getClientId(), formattedAy)) {
            throw new DuplicateResourceException("ITR Return", "assessmentYear", formattedAy + " for Client " + client.getDisplayName());
        }

        Optional<ItrProfileEntity> profileOpt = itrProfileRepository.findByOrganizationIdAndClientId(organizationId, request.getClientId());

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

        ItrReturnEntity entity = ItrReturnEntity.builder()
                .clientId(request.getClientId())
                .itrProfileId(profileOpt.map(ItrProfileEntity::getId).orElse(null))
                .assessmentYear(formattedAy)
                .financialYear(request.getFinancialYear().trim())
                .itrType(request.getItrType())
                .taxpayerType(taxpayerType)
                .dueDate(dueDate)
                .status(request.getStatus() != null ? request.getStatus() : ItrStatus.DOCUMENTS_PENDING)
                .assignedEmployeeId(assignedEmpId)
                .notes(request.getNotes())
                .build();
        entity.setOrganizationId(organizationId);

        ItrReturnEntity saved = itrReturnRepository.save(entity);
        log.info("Created ITR Return: id={}, client={}, AY={} for tenant={}", saved.getId(), client.getDisplayName(), saved.getAssessmentYear(), organizationId);
        return enrichReturnDto(saved);
    }

    @Override
    @Transactional
    public ItrReturnDto updateReturn(UUID id, UpdateItrReturnRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity itrReturn = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        if (request.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(request.getAssignedEmployeeId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned Employee", "id", request.getAssignedEmployeeId()));
            itrReturn.setAssignedEmployeeId(request.getAssignedEmployeeId());
        }

        itrReturn.setItrType(request.getItrType());
        if (request.getTaxpayerType() != null) {
            itrReturn.setTaxpayerType(request.getTaxpayerType());
        }
        if (request.getDueDate() != null) {
            itrReturn.setDueDate(request.getDueDate());
        }
        if (request.getStatus() != null) {
            itrReturn.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getNotes())) {
            itrReturn.setNotes(request.getNotes().trim());
        }

        ItrReturnEntity saved = itrReturnRepository.save(itrReturn);
        log.info("Updated ITR Return: id={} for tenant={}", saved.getId(), organizationId);
        return enrichReturnDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ItrReturnDto getReturnById(UUID id) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity itrReturn = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        return enrichReturnDto(itrReturn);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ItrReturnDto> getReturns(ItrFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<ItrReturnEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

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

            if (filterRequest.getTaxpayerType() != null) {
                predicates.add(cb.equal(root.get("taxpayerType"), filterRequest.getTaxpayerType()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }

            if (filterRequest.getAssignedEmployeeId() != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), filterRequest.getAssignedEmployeeId()));
            }

            if (Boolean.TRUE.equals(filterRequest.getIsOverdue())) {
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(root.get("status").in(ItrStatus.DOCUMENTS_PENDING, ItrStatus.DATA_ENTRY, ItrStatus.UNDER_REVIEW, ItrStatus.READY_TO_FILE));
            }

            if (Boolean.TRUE.equals(filterRequest.getIsUpcoming())) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), LocalDate.now()));
            }

            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("assessmentYear")), pattern),
                        cb.like(cb.lower(root.get("acknowledgementNumber")), pattern)
                ));
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
        ItrReturnEntity itrReturn = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        itrReturn.setStatus(request.getStatus());

        if (request.getStatus() == ItrStatus.FILED && itrReturn.getFilingDate() == null) {
            itrReturn.setFilingDate(LocalDate.now());
        }

        if (StringUtils.hasText(request.getNotes())) {
            String currentNotes = StringUtils.hasText(itrReturn.getNotes()) ? itrReturn.getNotes() + "\n" : "";
            itrReturn.setNotes(currentNotes + "[" + LocalDate.now() + " Status -> " + request.getStatus() + "]: " + request.getNotes().trim());
        }

        ItrReturnEntity saved = itrReturnRepository.save(itrReturn);
        log.info("Updated ITR Return status: id={}, newStatus={} for tenant={}", id, request.getStatus(), organizationId);
        return enrichReturnDto(saved);
    }

    @Override
    @Transactional
    public ItrReturnDto recordFilingDetails(UUID id, RecordItrFilingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity itrReturn = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        itrReturn.setFilingDate(request.getFilingDate() != null ? request.getFilingDate() : LocalDate.now());
        itrReturn.setAcknowledgementNumber(request.getAcknowledgementNumber().trim());

        if (request.getVerificationDate() != null) {
            itrReturn.setVerificationDate(request.getVerificationDate());
            itrReturn.setStatus(ItrStatus.COMPLETED);
        } else {
            itrReturn.setStatus(ItrStatus.VERIFICATION_PENDING);
        }

        if (StringUtils.hasText(request.getNotes())) {
            String currentNotes = StringUtils.hasText(itrReturn.getNotes()) ? itrReturn.getNotes() + "\n" : "";
            itrReturn.setNotes(currentNotes + "[Filing Details Recorded]: " + request.getNotes().trim());
        }

        ItrReturnEntity saved = itrReturnRepository.save(itrReturn);
        log.info("Recorded ITR filing details: id={}, ackNo={}, status={} for tenant={}", id, request.getAcknowledgementNumber(), saved.getStatus(), organizationId);
        return enrichReturnDto(saved);
    }

    @Override
    @Transactional
    public ItrReturnDto assignEmployee(UUID id, AssignItrEmployeeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ItrReturnEntity itrReturn = itrReturnRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ITR Return", "id", id));

        employeeRepository.findByIdAndOrganizationId(request.getEmployeeId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        itrReturn.setAssignedEmployeeId(request.getEmployeeId());
        ItrReturnEntity saved = itrReturnRepository.save(itrReturn);
        log.info("Assigned employee {} to ITR Return {} for tenant {}", request.getEmployeeId(), id, organizationId);
        return enrichReturnDto(saved);
    }

    // =========================================================================
    // 3. Upcoming, Overdue, History & Workload Dashboard
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ItrReturnDto> getUpcomingReturns(int daysAhead) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate now = LocalDate.now();
        LocalDate targetDate = now.plusDays(daysAhead > 0 ? daysAhead : 30);

        Specification<ItrReturnEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("organizationId"), organizationId),
                cb.between(root.get("dueDate"), now, targetDate),
                root.get("status").in(ItrStatus.DOCUMENTS_PENDING, ItrStatus.DATA_ENTRY, ItrStatus.UNDER_REVIEW, ItrStatus.READY_TO_FILE)
        );

        return itrReturnRepository.findAll(spec).stream().map(this::enrichReturnDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItrReturnDto> getOverdueReturns() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        LocalDate now = LocalDate.now();

        Specification<ItrReturnEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("organizationId"), organizationId),
                cb.lessThan(root.get("dueDate"), now),
                root.get("status").in(ItrStatus.DOCUMENTS_PENDING, ItrStatus.DATA_ENTRY, ItrStatus.UNDER_REVIEW, ItrStatus.READY_TO_FILE)
        );

        return itrReturnRepository.findAll(spec).stream().map(this::enrichReturnDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItrReturnDto> getClientItrHistory(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        List<ItrReturnEntity> returns = itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(organizationId, clientId);
        return returns.stream().map(this::enrichReturnDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ItrWorkloadDashboardDto getWorkloadDashboard(String assessmentYear, UUID assignedEmployeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        String targetAy = StringUtils.hasText(assessmentYear) ? assessmentYear.trim() : deriveCurrentAssessmentYear();

        Specification<ItrReturnEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            predicates.add(cb.equal(root.get("assessmentYear"), targetAy));

            if (assignedEmployeeId != null) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), assignedEmployeeId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ItrReturnEntity> returns = itrReturnRepository.findAll(spec);

        long docPending = 0;
        long dataEntry = 0;
        long underReview = 0;
        long readyToFile = 0;
        long filed = 0;
        long verificationPending = 0;
        long completed = 0;
        long overdueCount = 0;
        long upcomingCount = 0;

        LocalDate today = LocalDate.now();
        List<ItrClientWorkloadItem> clientItems = new ArrayList<>();

        for (ItrReturnEntity ret : returns) {
            switch (ret.getStatus()) {
                case DOCUMENTS_PENDING -> docPending++;
                case DATA_ENTRY -> dataEntry++;
                case UNDER_REVIEW -> underReview++;
                case READY_TO_FILE -> readyToFile++;
                case FILED -> filed++;
                case VERIFICATION_PENDING -> verificationPending++;
                case COMPLETED -> completed++;
                case CANCELLED -> {}
            }

            boolean isOverdue = ret.getDueDate() != null
                    && ret.getDueDate().isBefore(today)
                    && (ret.getStatus() == ItrStatus.DOCUMENTS_PENDING || ret.getStatus() == ItrStatus.DATA_ENTRY
                    || ret.getStatus() == ItrStatus.UNDER_REVIEW || ret.getStatus() == ItrStatus.READY_TO_FILE);

            if (isOverdue) {
                overdueCount++;
            } else if (ret.getDueDate() != null && !ret.getDueDate().isBefore(today)) {
                upcomingCount++;
            }

            ClientEntity client = clientRepository.findByIdAndOrganizationId(ret.getClientId(), organizationId).orElse(null);
            String clientName = client != null ? client.getDisplayName() : "Unknown Client";
            String pan = client != null ? client.getPan() : null;

            String assignedTo = "Unassigned";
            if (ret.getAssignedEmployeeId() != null) {
                Optional<EmployeeEntity> emp = employeeRepository.findByIdAndOrganizationId(ret.getAssignedEmployeeId(), organizationId);
                if (emp.isPresent()) {
                    assignedTo = emp.get().getFullName();
                }
            }

            clientItems.add(ItrClientWorkloadItem.builder()
                    .returnId(ret.getId())
                    .clientId(ret.getClientId())
                    .clientName(clientName)
                    .pan(pan)
                    .assessmentYear(ret.getAssessmentYear())
                    .itrType(ret.getItrType())
                    .taxpayerType(ret.getTaxpayerType())
                    .dueDate(ret.getDueDate())
                    .status(ret.getStatus())
                    .assignedEmployeeId(ret.getAssignedEmployeeId())
                    .assignedTo(assignedTo)
                    .isOverdue(isOverdue)
                    .build());
        }

        return ItrWorkloadDashboardDto.builder()
                .assessmentYear(targetAy)
                .totalReturns(returns.size())
                .documentsPendingCount(docPending)
                .dataEntryCount(dataEntry)
                .underReviewCount(underReview)
                .readyToFileCount(readyToFile)
                .filedCount(filed)
                .verificationPendingCount(verificationPending)
                .completedCount(completed)
                .overdueCount(overdueCount)
                .upcomingCount(upcomingCount)
                .returns(clientItems)
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ItrProfileDto enrichProfileDto(ItrProfileEntity profile) {
        ItrProfileDto dto = itrMapper.toProfileDto(profile);
        clientRepository.findByIdAndOrganizationId(profile.getClientId(), profile.getOrganizationId())
                .ifPresent(c -> dto.setClientName(c.getDisplayName()));

        if (profile.getAssignedEmployeeId() != null) {
            employeeRepository.findByIdAndOrganizationId(profile.getAssignedEmployeeId(), profile.getOrganizationId())
                    .ifPresent(e -> dto.setAssignedEmployeeName(e.getFullName()));
        }
        return dto;
    }

    private ItrReturnDto enrichReturnDto(ItrReturnEntity entity) {
        ItrReturnDto dto = itrMapper.toReturnDto(entity);
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
}
