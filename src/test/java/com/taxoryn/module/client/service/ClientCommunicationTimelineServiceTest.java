package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientCommunicationRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.entity.ClientNoteEntity.Visibility;
import com.taxoryn.module.client.repository.ClientCommunicationTimelineRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCommunicationTimelineServiceTest {
    @Mock private ClientCommunicationTimelineRepository timelineRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ClientService clientService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @InjectMocks private ClientCommunicationTimelineService service;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUpPracticeUser() {
        SecurityUser principal = SecurityUser.builder().userId(userId).organizationId(organizationId)
                .email("staff@example.com").roles(Set.of("ROLE_PRACTITIONER"))
                .permissions(Set.of("CLIENT_COMMUNICATION_VIEW", "CLIENT_COMMUNICATION_CREATE", "CLIENT_COMMUNICATION_UPDATE"))
                .enabled(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        TenantContext.setTenantId(organizationId);
        org.mockito.Mockito.lenient().when(clientService.getClientById(clientId)).thenReturn(ClientDto.builder().id(clientId).build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPersistsCommunicationWithAuthenticatedTenantAndFollowUp() {
        Instant occurredAt = Instant.parse("2026-10-02T10:30:00Z");
        ClientCommunicationRequest request = request(NoteType.PHONE_CALL, occurredAt, true,
                Instant.parse("2026-10-05T10:00:00Z"));
        when(timelineRepository.save(any(ClientNoteEntity.class))).thenAnswer(invocation -> {
            ClientNoteEntity entry = invocation.getArgument(0);
            entry.setId(UUID.randomUUID());
            return entry;
        });

        service.create(clientId, request);

        ArgumentCaptor<ClientNoteEntity> captor = ArgumentCaptor.forClass(ClientNoteEntity.class);
        verify(timelineRepository).save(captor.capture());
        ClientNoteEntity saved = captor.getValue();
        assertEquals(organizationId, saved.getOrganizationId());
        assertEquals(clientId, saved.getClientId());
        assertEquals(userId, saved.getAuthorId());
        assertEquals(NoteType.PHONE_CALL, saved.getNoteType());
        assertEquals(occurredAt, saved.getOccurredAt());
        assertEquals(Visibility.INTERNAL, saved.getVisibility());
        assertEquals(true, saved.isFollowUpRequired());
        assertEquals("Please share the missing GST documents.", saved.getContent());
    }

    @Test
    void rejectsFollowUpWithoutDate() {
        assertThrows(BadRequestException.class,
                () -> service.create(clientId, request(NoteType.NOTE, Instant.now(), true, null)));
        verify(timelineRepository, never()).save(any());
    }

    @Test
    void enforcesClientScopeBeforeQueryingEntries() {
        when(clientService.getClientById(clientId)).thenThrow(new AccessDeniedException("outside scope"));

        assertThrows(AccessDeniedException.class, () -> service.listPracticeEntries(clientId, null, null, null, null, 0, 20));
        verify(timelineRepository, never()).findTimelineEntries(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void capsPageSizeAndReturnsNewestFirstPageRequest() {
        when(timelineRepository.findTimelineEntries(eq(organizationId), eq(clientId), eq(NoteType.EMAIL), any(), any(),
                eq(false), eq(null), any())).thenReturn(new PageImpl<>(List.of()));

        service.listPracticeEntries(clientId, NoteType.EMAIL, null, null, false, 0, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(timelineRepository).findTimelineEntries(eq(organizationId), eq(clientId), eq(NoteType.EMAIL), any(), any(),
                eq(false), eq(null), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals("occurredAt", pageable.getValue().getSort().toList().get(0).getProperty());
        assertEquals(org.springframework.data.domain.Sort.Direction.DESC, pageable.getValue().getSort().toList().get(0).getDirection());
    }

    @Test
    void clientPortalQueryIsRestrictedToClientVisibleRowsAndOwnClientId() {
        SecurityUser portalPrincipal = SecurityUser.builder().userId(userId).organizationId(organizationId).clientId(clientId)
                .email("client@example.com").roles(Set.of("ROLE_CLIENT_USER")).permissions(Set.of("CLIENT_PORTAL_ACCESS")).enabled(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(portalPrincipal, null, portalPrincipal.getAuthorities()));
        when(clientRepository.findByIdAndOrganizationId(clientId, organizationId)).thenReturn(Optional.of(ClientEntity.builder().displayName("Client").build()));
        when(timelineRepository.findTimelineEntries(eq(organizationId), eq(clientId), eq(null), eq(null), eq(null), eq(null),
                eq(Visibility.CLIENT_VISIBLE), any())).thenReturn(new PageImpl<>(List.of()));

        service.listClientVisibleEntries(clientId, 0, 20);

        verify(timelineRepository).findTimelineEntries(eq(organizationId), eq(clientId), eq(null), eq(null), eq(null), eq(null),
                eq(Visibility.CLIENT_VISIBLE), any());
        UUID anotherClient = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> service.listClientVisibleEntries(anotherClient, 0, 20));
    }

    private ClientCommunicationRequest request(NoteType type, Instant occurredAt, boolean followUp, Instant followUpDate) {
        ClientCommunicationRequest request = new ClientCommunicationRequest();
        request.setCommunicationType(type);
        request.setSubject("GST discussion");
        request.setContent(" Please share the missing GST documents. ");
        request.setOccurredAt(occurredAt);
        request.setVisibility(Visibility.INTERNAL);
        request.setFollowUpRequired(followUp);
        request.setFollowUpDate(followUpDate);
        return request;
    }
}
