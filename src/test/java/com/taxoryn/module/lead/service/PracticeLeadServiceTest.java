package com.taxoryn.module.lead.service;

import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.service.ClientService;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.lead.dto.ConvertPracticeLeadRequest;
import com.taxoryn.module.lead.dto.PracticeLeadRequest;
import com.taxoryn.module.lead.entity.PracticeLeadEntity;
import com.taxoryn.module.lead.repository.PracticeLeadActivityRepository;
import com.taxoryn.module.lead.repository.PracticeLeadRepository;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeLeadServiceTest {
    @Mock private PracticeLeadRepository leadRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private PracticeLeadActivityRepository activityRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientService clientService;
    @Mock private AuditService auditService;
    @InjectMocks private PracticeLeadService service;
    private final UUID organizationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach void authenticate() {
        SecurityUser principal = SecurityUser.builder().userId(userId).organizationId(organizationId).email("owner@example.com")
                .roles(Set.of("ORG_ADMIN")).permissions(Set.of("LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_ASSIGN", "LEAD_CONVERT", "LEAD_DELETE", "CLIENT_CREATE")).enabled(true).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        TenantContext.setTenantId(organizationId);
    }
    @AfterEach void cleanup() { TenantContext.clear(); SecurityContextHolder.clearContext(); }

    @Test void createUsesAuthenticatedOrganizationAndDoesNotCreateClient() {
        PracticeLeadRequest request = new PracticeLeadRequest(); request.setName("Rahul Sharma"); request.setPriority(PracticeLeadEntity.LeadPriority.CRITICAL); request.setNextFollowUpAt(Instant.parse("2026-10-05T09:00:00Z"));
        when(leadRepository.save(any())).thenAnswer(invocation -> { PracticeLeadEntity entity = invocation.getArgument(0); entity.setId(UUID.randomUUID()); return entity; });
        var created = service.create(request);
        ArgumentCaptor<PracticeLeadEntity> captor = ArgumentCaptor.forClass(PracticeLeadEntity.class);
        verify(leadRepository).save(captor.capture());
        assertEquals(organizationId, captor.getValue().getOrganizationId());
        assertEquals(Instant.parse("2026-10-05T09:00:00Z"), created.getNextFollowUpAt());
        assertEquals(PracticeLeadEntity.LeadPriority.CRITICAL, created.getPriority());
        verify(clientService, never()).createClient(any());
    }

    @Test void crossOrganizationLeadIsNotFound() {
        UUID leadId = UUID.randomUUID(); when(leadRepository.findByIdAndOrganizationId(leadId, organizationId)).thenReturn(Optional.empty());
        assertThrows(com.taxoryn.core.exception.ResourceNotFoundException.class, () -> service.get(leadId));
    }

    @Test void conversionUsesClientServiceAndStoresConvertedClient() {
        UUID leadId = UUID.randomUUID(), clientId = UUID.randomUUID();
        PracticeLeadEntity lead = PracticeLeadEntity.builder().name("Rahul Sharma").businessName("Rahul Consulting")
                .status(PracticeLeadEntity.LeadStatus.QUALIFIED).leadType(PracticeLeadEntity.LeadType.BUSINESS).build();
        lead.setId(leadId); lead.setOrganizationId(organizationId);
        when(leadRepository.findByIdAndOrganizationId(leadId, organizationId)).thenReturn(Optional.of(lead));
        when(clientService.createClient(any())).thenReturn(ClientDto.builder().id(clientId).build());
        when(leadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ConvertPracticeLeadRequest request = new ConvertPracticeLeadRequest();
        request.setClient(new com.taxoryn.module.client.dto.CreateClientRequest());
        var converted = service.convert(leadId, request);
        assertEquals(clientId, converted.getConvertedClientId());
        assertEquals(PracticeLeadEntity.LeadStatus.CONVERTED, converted.getStatus());
        verify(clientService).createClient(argThat(client -> client.getDisplayName().equals("Rahul Sharma") && client.getClientType() == com.taxoryn.module.client.entity.ClientEntity.ClientType.OTHER));
    }

    @Test void duplicateConversionIsRejected() {
        UUID leadId = UUID.randomUUID(); PracticeLeadEntity lead = PracticeLeadEntity.builder().name("Already converted")
                .status(PracticeLeadEntity.LeadStatus.CONVERTED).convertedClientId(UUID.randomUUID()).build();
        lead.setId(leadId); lead.setOrganizationId(organizationId);
        when(leadRepository.findByIdAndOrganizationId(leadId, organizationId)).thenReturn(Optional.of(lead));
        assertThrows(DuplicateResourceException.class, () -> service.convert(leadId, new ConvertPracticeLeadRequest()));
        verify(clientService, never()).createClient(any());
    }

    @Test void invalidStatusJumpIsRejected() {
        UUID leadId = UUID.randomUUID();
        PracticeLeadEntity lead = PracticeLeadEntity.builder().name("Rahul Sharma").status(PracticeLeadEntity.LeadStatus.NEW).build();
        lead.setId(leadId); lead.setOrganizationId(organizationId);
        when(leadRepository.findByIdAndOrganizationId(leadId, organizationId)).thenReturn(Optional.of(lead));
        PracticeLeadRequest request = new PracticeLeadRequest(); request.setName("Rahul Sharma"); request.setStatus(PracticeLeadEntity.LeadStatus.PROPOSAL_SENT);
        assertThrows(com.taxoryn.core.exception.BadRequestException.class, () -> service.update(leadId, request));
    }

    @Test void assignmentRejectsEmployeeFromOtherOrganization() {
        UUID leadId = UUID.randomUUID(), employeeId = UUID.randomUUID();
        PracticeLeadEntity lead = PracticeLeadEntity.builder().name("Rahul Sharma").status(PracticeLeadEntity.LeadStatus.NEW).build();
        lead.setId(leadId); lead.setOrganizationId(organizationId);
        when(leadRepository.findByIdAndOrganizationId(leadId, organizationId)).thenReturn(Optional.of(lead));
        when(employeeRepository.findByIdAndOrganizationId(employeeId, organizationId)).thenReturn(Optional.empty());
        assertThrows(com.taxoryn.core.exception.ResourceNotFoundException.class, () -> service.assign(leadId, employeeId));
        verify(leadRepository, never()).save(any());
    }
}
