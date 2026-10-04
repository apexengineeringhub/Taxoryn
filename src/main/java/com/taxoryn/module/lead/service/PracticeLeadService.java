package com.taxoryn.module.lead.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.service.ClientService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.lead.dto.ConvertPracticeLeadRequest;
import com.taxoryn.module.lead.dto.PracticeLeadDto;
import com.taxoryn.module.lead.dto.PracticeLeadRequest;
import com.taxoryn.module.lead.dto.PracticeLeadActivityDto;
import com.taxoryn.module.lead.dto.PracticeLeadActivityRequest;
import com.taxoryn.module.lead.entity.PracticeLeadEntity;
import com.taxoryn.module.lead.entity.PracticeLeadEntity.*;
import com.taxoryn.module.lead.entity.PracticeLeadActivityEntity;
import com.taxoryn.module.lead.entity.PracticeLeadActivityEntity.ActivityType;
import com.taxoryn.module.lead.repository.PracticeLeadActivityRepository;
import com.taxoryn.module.lead.repository.PracticeLeadRepository;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PracticeLeadService {
    private static final int MAX_PAGE_SIZE = 100;
    private final PracticeLeadRepository leadRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final PracticeLeadActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ClientService clientService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<PracticeLeadDto> list(LeadStatus status, LeadPriority priority, LeadSource source,
            UUID assignedEmployeeId, String serviceCode, String search, int page, int size) {
        UUID organizationId = requireOrganization();
        UUID scopeEmployee = currentEmployeeScope(organizationId);
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        var result = leadRepository.findLeads(organizationId, status, priority, source,
                assignedEmployeeId, trimToNull(serviceCode), trimToNull(search), scopeEmployee,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"))));
        var pageLeads = result.getContent();
        var employeeIds = pageLeads.stream().map(PracticeLeadEntity::getAssignedEmployeeId).filter(java.util.Objects::nonNull).distinct().toList();
        var serviceCodes = pageLeads.stream().map(PracticeLeadEntity::getInterestedServiceCode).filter(StringUtils::hasText).distinct().toList();
        var employeeNames = employeeIds.isEmpty() ? java.util.Map.<UUID, String>of() : employeeRepository.findAllByOrganizationIdAndIdIn(organizationId, employeeIds).stream()
                .collect(java.util.stream.Collectors.toMap(EmployeeEntity::getId, EmployeeEntity::getFullName));
        var serviceNames = serviceCodes.isEmpty() ? java.util.Map.<String, String>of() : serviceRepository.findAccessibleServicesByCodes(serviceCodes, organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.getServiceCode(), s -> s.getServiceName(), (a, b) -> a));
        return PagedResponse.of(result, lead -> toDto(lead, employeeNames, serviceNames));
    }

    @Transactional(readOnly = true)
    public PracticeLeadDto get(UUID id) { return toDto(accessibleLead(id)); }

    @Transactional(readOnly = true)
    public java.util.List<PracticeLeadActivityDto> getActivities(UUID id) {
        PracticeLeadEntity lead = accessibleLead(id);
        return activityRepository.findTop100ByOrganizationIdAndLeadIdOrderByOccurredAtDescCreatedAtDesc(lead.getOrganizationId(), id)
                .stream().map(this::toActivityDto).toList();
    }

    @Transactional
    public PracticeLeadActivityDto addActivity(UUID id, PracticeLeadActivityRequest request) {
        PracticeLeadEntity lead = accessibleLead(id);
        if (request.getActivityType() == ActivityType.SYSTEM_EVENT) throw new BadRequestException("System events cannot be manually created.");
        UUID userId = SecurityUtils.getCurrentUserId();
        String authorName = userId == null ? "Practice user" : userRepository.findByIdAndOrganizationId(userId, lead.getOrganizationId())
                .map(u -> u.getFullName()).filter(StringUtils::hasText).orElse("Practice user");
        PracticeLeadActivityEntity activity = PracticeLeadActivityEntity.builder().leadId(id).activityType(request.getActivityType())
                .subject(trimToNull(request.getSubject())).content(request.getContent().trim()).occurredAt(request.getOccurredAt()).authorName(authorName).build();
        activity.setOrganizationId(lead.getOrganizationId());
        PracticeLeadActivityEntity saved = activityRepository.save(activity);
        auditService.logEvent("LEAD_COMMUNICATION_RECORDED", "PRACTICE_LEAD", id.toString(), null, request.getActivityType().name());
        return toActivityDto(saved);
    }

    @Transactional
    public PracticeLeadDto create(PracticeLeadRequest request) {
        UUID organizationId = requireOrganization();
        if (request.getStatus() != null && request.getStatus() != LeadStatus.NEW)
            throw new BadRequestException("New leads must start in NEW status.");
        UUID currentEmployee = currentEmployeeScope(organizationId);
        UUID assigned = request.getAssignedEmployeeId();
        if (assigned != null) {
            requireEmployee(assigned, organizationId);
            if (!assigned.equals(currentEmployee) && !SecurityUtils.hasAuthority("LEAD_ASSIGN")) throw new AccessDeniedException("Lead assignment permission is required.");
        }
        else if (currentEmployee != null) assigned = currentEmployee;
        validateService(request.getInterestedServiceCode(), organizationId);
        PracticeLeadEntity lead = PracticeLeadEntity.builder()
                .leadType(request.getLeadType() == null ? LeadType.INDIVIDUAL : request.getLeadType())
                .name(request.getName().trim()).businessName(trimToNull(request.getBusinessName()))
                .email(normalizeEmail(request.getEmail())).phone(trimToNull(request.getPhone()))
                .source(request.getSource() == null ? LeadSource.OTHER : request.getSource())
                .status(LeadStatus.NEW).priority(request.getPriority() == null ? LeadPriority.MEDIUM : request.getPriority())
                .interestedServiceCode(trimToNull(request.getInterestedServiceCode()))
                .description(trimToNull(request.getDescription())).assignedEmployeeId(assigned)
                .nextFollowUpAt(request.getNextFollowUpAt()).build();
        lead.setOrganizationId(organizationId);
        PracticeLeadEntity saved = leadRepository.save(lead);
        auditService.logEvent("LEAD_CREATED", "PRACTICE_LEAD", saved.getId().toString(), null, "Lead created");
        return toDto(saved);
    }

    @Transactional
    public PracticeLeadDto update(UUID id, PracticeLeadRequest request) {
        PracticeLeadEntity lead = accessibleLead(id);
        if (lead.getStatus() == LeadStatus.CONVERTED || lead.getStatus() == LeadStatus.LOST)
            throw new BadRequestException("Converted or lost leads cannot be changed.");
        validateService(request.getInterestedServiceCode(), lead.getOrganizationId());
        if (request.getAssignedEmployeeId() != null) {
            requireEmployee(request.getAssignedEmployeeId(), lead.getOrganizationId());
            if (!request.getAssignedEmployeeId().equals(lead.getAssignedEmployeeId()) && !SecurityUtils.hasAuthority("LEAD_ASSIGN")) throw new AccessDeniedException("Lead assignment permission is required.");
        }
        if (request.getStatus() != null && request.getStatus() != lead.getStatus()) validateTransition(lead.getStatus(), request.getStatus());
        if (request.getStatus() == LeadStatus.LOST && request.getStatus() != lead.getStatus())
            throw new BadRequestException("Use the mark-lost action to record a lead loss reason.");
        String oldStatus = lead.getStatus().name();
        lead.setLeadType(request.getLeadType() == null ? lead.getLeadType() : request.getLeadType());
        lead.setName(request.getName().trim()); lead.setBusinessName(trimToNull(request.getBusinessName()));
        lead.setEmail(normalizeEmail(request.getEmail())); lead.setPhone(trimToNull(request.getPhone()));
        if (request.getSource() != null) lead.setSource(request.getSource());
        if (request.getStatus() != null) lead.setStatus(request.getStatus());
        if (request.getPriority() != null) lead.setPriority(request.getPriority());
        lead.setInterestedServiceCode(trimToNull(request.getInterestedServiceCode()));
        lead.setDescription(trimToNull(request.getDescription())); lead.setNextFollowUpAt(request.getNextFollowUpAt());
        if (request.getLostReason() != null) lead.setLostReason(trimToNull(request.getLostReason()));
        PracticeLeadEntity saved = leadRepository.save(lead);
        auditService.logEvent(oldStatus.equals(saved.getStatus().name()) ? "LEAD_UPDATED" : "LEAD_STATUS_CHANGED",
                "PRACTICE_LEAD", saved.getId().toString(), oldStatus, saved.getStatus().name());
        return toDto(saved);
    }

    @Transactional
    public PracticeLeadDto assign(UUID id, UUID employeeId) {
        PracticeLeadEntity lead = accessibleLead(id);
        if (employeeId == null) throw new BadRequestException("Employee is required for lead assignment.");
        EmployeeEntity employee = requireEmployee(employeeId, lead.getOrganizationId());
        lead.setAssignedEmployeeId(employee.getId());
        PracticeLeadEntity saved = leadRepository.save(lead);
        auditService.logEvent("LEAD_ASSIGNED", "PRACTICE_LEAD", id.toString(), null, "Assigned to " + employee.getFullName());
        return toDto(saved);
    }

    @Transactional
    public PracticeLeadDto markLost(UUID id, String reason) {
        PracticeLeadEntity lead = accessibleLead(id);
        validateTransition(lead.getStatus(), LeadStatus.LOST);
        lead.setStatus(LeadStatus.LOST); lead.setLostReason(trimToNull(reason));
        PracticeLeadEntity saved = leadRepository.save(lead);
        auditService.logEvent("LEAD_MARKED_LOST", "PRACTICE_LEAD", id.toString(), null, trimToNull(reason));
        return toDto(saved);
    }

    @Transactional
    public PracticeLeadDto convert(UUID id, ConvertPracticeLeadRequest request) {
        PracticeLeadEntity lead = accessibleLead(id);
        if (lead.getConvertedClientId() != null || lead.getStatus() == LeadStatus.CONVERTED)
            throw new com.taxoryn.core.exception.DuplicateResourceException("Lead has already been converted to a Client.");
        if (!(lead.getStatus() == LeadStatus.QUALIFIED || lead.getStatus() == LeadStatus.PROPOSAL_SENT || lead.getStatus() == LeadStatus.FOLLOW_UP))
            throw new BadRequestException("Only qualified, proposal-sent, or follow-up leads can be converted.");
        CreateClientRequest clientRequest = request.getClient();
        if (clientRequest.getClientType() == null) clientRequest.setClientType(inferClientType(lead));
        if (!StringUtils.hasText(clientRequest.getDisplayName())) clientRequest.setDisplayName(lead.getName());
        if (!StringUtils.hasText(clientRequest.getLegalName())) clientRequest.setLegalName(lead.getBusinessName());
        if (!StringUtils.hasText(clientRequest.getEmail())) clientRequest.setEmail(lead.getEmail());
        if (!StringUtils.hasText(clientRequest.getPhone())) clientRequest.setPhone(lead.getPhone());
        ClientDto client = clientService.createClient(clientRequest);
        lead.setConvertedClientId(client.getId()); lead.setConvertedAt(Instant.now());
        lead.setConvertedBy(SecurityUtils.getCurrentUserId()); lead.setStatus(LeadStatus.CONVERTED);
        PracticeLeadEntity saved = leadRepository.save(lead);
        auditService.logEvent("LEAD_CONVERTED", "PRACTICE_LEAD", id.toString(), null, "Converted to client " + client.getId());
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PracticeLeadEntity lead = accessibleLead(id);
        if (lead.getStatus() == LeadStatus.CONVERTED) throw new BadRequestException("Converted leads must be retained as historical records.");
        leadRepository.delete(lead);
        auditService.logEvent("LEAD_DELETED", "PRACTICE_LEAD", id.toString(), null, null);
    }

    private PracticeLeadEntity accessibleLead(UUID id) {
        UUID organizationId = requireOrganization();
        PracticeLeadEntity lead = leadRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));
        UUID scopeEmployee = currentEmployeeScope(organizationId);
        if (scopeEmployee != null && !scopeEmployee.equals(lead.getAssignedEmployeeId())) throw new AccessDeniedException("Lead is outside your assigned scope.");
        return lead;
    }

    private UUID currentEmployeeScope(UUID organizationId) {
        if (SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.hasRole("TAXORYN_SUPERADMIN")
                || SecurityUtils.hasRole("ORG_ADMIN") || SecurityUtils.hasRole("PRACTICE_ADMIN")
                || SecurityUtils.hasRole("PRACTICE_OWNER") || SecurityUtils.hasRole("PARTNER")
                || SecurityUtils.hasRole("CA_PARTNER")
                || SecurityUtils.hasRole("MANAGER") || SecurityUtils.hasRole("TAX_MANAGER")
                || SecurityUtils.hasRole("PRACTITIONER") || SecurityUtils.hasRole("TAX_PROFESSIONAL")
                || SecurityUtils.hasRole("SENIOR_TAX_ASSOCIATE")) {
            return null;
        }
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return null;
        return employeeRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(EmployeeEntity::getId)
                .orElse(null);
    }

    private UUID requireOrganization() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) throw new UnauthorizedException("Authenticated organization context is required.");
        return organizationId;
    }

    private EmployeeEntity requireEmployee(UUID id, UUID organizationId) {
        return employeeRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private void validateService(String code, UUID organizationId) {
        if (StringUtils.hasText(code) && serviceRepository.findAccessibleServiceByCode(code.trim(), organizationId).isEmpty())
            throw new ResourceNotFoundException("Service", "serviceCode", code);
    }

    private void validateTransition(LeadStatus current, LeadStatus target) {
        boolean allowed = switch (current) {
            case NEW -> target == LeadStatus.CONTACTED || target == LeadStatus.LOST;
            case CONTACTED -> target == LeadStatus.QUALIFIED || target == LeadStatus.FOLLOW_UP || target == LeadStatus.LOST;
            case QUALIFIED -> target == LeadStatus.PROPOSAL_SENT || target == LeadStatus.FOLLOW_UP || target == LeadStatus.LOST;
            case PROPOSAL_SENT -> target == LeadStatus.FOLLOW_UP || target == LeadStatus.QUALIFIED || target == LeadStatus.LOST;
            case FOLLOW_UP -> target == LeadStatus.CONTACTED || target == LeadStatus.QUALIFIED || target == LeadStatus.PROPOSAL_SENT || target == LeadStatus.LOST;
            case CONVERTED, LOST -> false;
        };
        if (!allowed) throw new BadRequestException("Invalid lead status transition: " + current + " → " + target);
    }

    private ClientEntity.ClientType inferClientType(PracticeLeadEntity lead) {
        return lead.getLeadType() == LeadType.BUSINESS ? ClientEntity.ClientType.OTHER : ClientEntity.ClientType.INDIVIDUAL;
    }

    private String normalizeEmail(String email) { return StringUtils.hasText(email) ? email.trim().toLowerCase() : null; }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }

    private PracticeLeadDto toDto(PracticeLeadEntity lead) {
        String employeeName = lead.getAssignedEmployeeId() == null ? null : employeeRepository.findByIdAndOrganizationId(lead.getAssignedEmployeeId(), lead.getOrganizationId()).map(EmployeeEntity::getFullName).orElse(null);
        String serviceName = lead.getInterestedServiceCode() == null ? null : serviceRepository.findAccessibleServiceByCode(lead.getInterestedServiceCode(), lead.getOrganizationId()).map(s -> s.getServiceName()).orElse(null);
        return toDto(lead, employeeName, serviceName);
    }

    private PracticeLeadDto toDto(PracticeLeadEntity lead, java.util.Map<UUID, String> employeeNames, java.util.Map<String, String> serviceNames) {
        String employeeName = lead.getAssignedEmployeeId() == null ? null : employeeNames.get(lead.getAssignedEmployeeId());
        String serviceName = lead.getInterestedServiceCode() == null ? null : serviceNames.get(lead.getInterestedServiceCode());
        return toDto(lead, employeeName, serviceName);
    }

    private PracticeLeadDto toDto(PracticeLeadEntity lead, String employeeName, String serviceName) {
        return PracticeLeadDto.builder().id(lead.getId()).leadType(lead.getLeadType()).name(lead.getName()).businessName(lead.getBusinessName())
                .email(lead.getEmail()).phone(lead.getPhone()).source(lead.getSource()).status(lead.getStatus()).priority(lead.getPriority())
                .interestedServiceCode(lead.getInterestedServiceCode()).interestedServiceName(serviceName).description(lead.getDescription())
                .assignedEmployeeId(lead.getAssignedEmployeeId()).assignedEmployeeName(employeeName).nextFollowUpAt(lead.getNextFollowUpAt())
                .convertedClientId(lead.getConvertedClientId()).convertedAt(lead.getConvertedAt()).createdAt(lead.getCreatedAt()).lostReason(lead.getLostReason()).build();
    }

    private PracticeLeadActivityDto toActivityDto(PracticeLeadActivityEntity activity) {
        return PracticeLeadActivityDto.builder().id(activity.getId()).leadId(activity.getLeadId()).activityType(activity.getActivityType())
                .subject(activity.getSubject()).content(activity.getContent()).occurredAt(activity.getOccurredAt())
                .authorName(activity.getAuthorName()).createdAt(activity.getCreatedAt()).build();
    }
}
