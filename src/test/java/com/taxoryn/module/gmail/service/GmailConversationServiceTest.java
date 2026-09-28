package com.taxoryn.module.gmail.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.dto.GmailConversationDto;
import com.taxoryn.module.gmail.dto.GmailConversationFilterRequest;
import com.taxoryn.module.gmail.dto.GmailConversationUpdateDto;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailConversationServiceTest {

    @Mock
    private GmailConversationRepository conversationRepository;

    @Mock
    private GmailAccountRepository accountRepository;

    @Mock
    private GmailOAuthService gmailOAuthService;

    @Mock
    private GmailApiClient apiClient;

    @Mock
    private TokenEncryptionService encryptionService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private PracticeSecurityScopeEvaluator scopeEvaluator;

    @Mock
    private AuditService auditService;

    private GmailConversationServiceImpl conversationService;
    private UUID orgId;
    private UUID staffUserId;
    private PracticeSecurityScope adminScope;
    private PracticeSecurityScope staffScope;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        staffUserId = UUID.randomUUID();

        adminScope = PracticeSecurityScope.builder()
                .organizationId(orgId)
                .userId(UUID.randomUUID())
                .isFirmAdmin(true)
                .build();

        staffScope = PracticeSecurityScope.builder()
                .organizationId(orgId)
                .userId(staffUserId)
                .isFirmAdmin(false)
                .isStaff(true)
                .accessibleAssigneeIds(Set.of(staffUserId))
                .build();

        conversationService = new GmailConversationServiceImpl(
                conversationRepository,
                accountRepository,
                gmailOAuthService,
                apiClient,
                encryptionService,
                clientRepository,
                userRepository,
                locationRepository,
                scopeEvaluator,
                auditService
        );
    }

    @Test
    @DisplayName("Verify getConversations returns paginated results with admin scope")
    void testGetConversationsAdminScope() {
        GmailConversationFilterRequest filter = new GmailConversationFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        GmailConversationEntity conv = GmailConversationEntity.builder()
                .threadId("th_1")
                .subject("GST Return")
                .status(GmailConversationStatus.OPEN)
                .build();
        conv.setId(UUID.randomUUID());
        conv.setOrganizationId(orgId);

        when(conversationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conv)));

        PagedResponse<GmailConversationDto> response = conversationService.getConversations(filter, adminScope);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getSubject()).isEqualTo("GST Return");
    }

    @Test
    @DisplayName("Verify updateConversation transitions status and records audit log")
    void testUpdateConversationStatus() {
        UUID convId = UUID.randomUUID();
        GmailConversationEntity conv = GmailConversationEntity.builder()
                .threadId("th_2")
                .subject("ITR Notice")
                .status(GmailConversationStatus.OPEN)
                .assignedUserId(staffUserId)
                .build();
        conv.setId(convId);
        conv.setOrganizationId(orgId);

        when(conversationRepository.findByIdAndOrganizationId(convId, orgId))
                .thenReturn(Optional.of(conv));
        when(conversationRepository.save(any(GmailConversationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GmailConversationUpdateDto updateDto = GmailConversationUpdateDto.builder()
                .status(GmailConversationStatus.RESOLVED)
                .priority(GmailConversationPriority.HIGH)
                .build();

        GmailConversationDto result = conversationService.updateConversation(convId, updateDto, staffScope);

        assertThat(result.getStatus()).isEqualTo(GmailConversationStatus.RESOLVED);
        assertThat(result.getPriority()).isEqualTo(GmailConversationPriority.HIGH);
        assertThat(conv.getResolvedAt()).isNotNull();

        verify(auditService).logEvent(eq(orgId), eq(staffUserId), eq("GMAIL_CONVERSATION_STATUS_CHANGED"), eq("GMAIL_CONVERSATION"), eq(convId.toString()), eq("OPEN"), eq("RESOLVED"));
    }

    @Test
    @DisplayName("Verify assignConversation validates assignee organization membership")
    void testAssignConversationValidatesOrg() {
        UUID convId = UUID.randomUUID();
        UUID newAssigneeId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();

        GmailConversationEntity conv = GmailConversationEntity.builder()
                .threadId("th_3")
                .status(GmailConversationStatus.OPEN)
                .build();
        conv.setId(convId);
        conv.setOrganizationId(orgId);

        UserEntity otherOrgUser = UserEntity.builder()
                .organizationId(otherOrgId) // Cross-tenant user
                .firstName("Attacker")
                .build();

        when(conversationRepository.findByIdAndOrganizationId(convId, orgId))
                .thenReturn(Optional.of(conv));
        when(userRepository.findById(newAssigneeId))
                .thenReturn(Optional.of(otherOrgUser));

        assertThatThrownBy(() -> conversationService.assignConversation(convId, newAssigneeId, adminScope))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Assigned user does not belong to the active practice organization");
    }

    @Test
    @DisplayName("Verify linkClient associates client and location to conversation")
    void testLinkClient() {
        UUID convId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        GmailConversationEntity conv = GmailConversationEntity.builder()
                .threadId("th_4")
                .status(GmailConversationStatus.OPEN)
                .build();
        conv.setId(convId);
        conv.setOrganizationId(orgId);

        ClientEntity client = ClientEntity.builder()
                .displayName("Alpha Traders")
                .pan("ABCDE1234F")
                .locationId(locationId)
                .build();
        client.setId(clientId);

        when(conversationRepository.findByIdAndOrganizationId(convId, orgId))
                .thenReturn(Optional.of(conv));
        when(clientRepository.findByIdAndOrganizationId(clientId, orgId))
                .thenReturn(Optional.of(client));
        when(conversationRepository.save(any(GmailConversationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GmailConversationDto result = conversationService.linkClient(convId, clientId, adminScope);

        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getLocationId()).isEqualTo(locationId);

        verify(auditService).logEvent(eq(orgId), any(), eq("GMAIL_CONVERSATION_CLIENT_LINKED"), eq("GMAIL_CONVERSATION"), eq(convId.toString()), isNull(), eq(clientId.toString()));
    }
}
