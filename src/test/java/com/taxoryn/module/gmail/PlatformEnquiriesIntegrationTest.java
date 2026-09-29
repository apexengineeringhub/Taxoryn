package com.taxoryn.module.gmail;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.dto.GmailConversationDto;
import com.taxoryn.module.gmail.dto.GmailConversationFilterRequest;
import com.taxoryn.module.gmail.dto.GmailMessageViewDto;
import com.taxoryn.module.gmail.dto.GmailReplyRequest;
import com.taxoryn.module.gmail.dto.GmailThreadModels;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.service.GmailConversationServiceImpl;
import com.taxoryn.module.gmail.service.GmailOAuthService;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlatformEnquiriesIntegrationTest {

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

    private UUID platformOrgId;
    private UUID adminUserId;
    private PracticeSecurityScope adminScope;

    @BeforeEach
    void setUp() {
        platformOrgId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

        adminScope = PracticeSecurityScope.builder()
                .organizationId(platformOrgId)
                .userId(adminUserId)
                .isFirmAdmin(true)
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
    @DisplayName("Verify enquiry category classification into Practitioner vs Support vs General")
    void testEnquiryCategoryClassification() {
        // Practitioner Lead
        String pracCat = GmailConversationServiceImpl.deriveCategory("dr.smith@example.com", "info@taxoryn.com", "Practitioner Onboarding Enquiry", "Interested in joining the platform");
        assertThat(pracCat).isEqualTo("PRACTITIONER_ENQUIRY");

        // Support Request
        String suppCat = GmailConversationServiceImpl.deriveCategory("user@client.com", "support@taxoryn.com", "Issue with GST filing export", "I am getting an error when downloading");
        assertThat(suppCat).isEqualTo("SUPPORT_REQUEST");

        // General Enquiry
        String genCat = GmailConversationServiceImpl.deriveCategory("hello@visitor.com", "admin@taxoryn.com", "General Query", "What are your business hours?");
        assertThat(genCat).isEqualTo("GENERAL_ENQUIRY");
    }

    @Test
    @DisplayName("Verify getConversationMessages calls GmailApiClient on-demand without storing bodies in DB")
    void testGetConversationMessagesLive() {
        UUID convId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String threadId = "th_live_99";

        GmailConversationEntity conv = GmailConversationEntity.builder()
                .gmailAccountId(accountId)
                .threadId(threadId)
                .subject("Onboarding Demo Request")
                .senderEmail("lead@practice.com")
                .senderName("Dr. Sharma")
                .recipientEmails("info@taxoryn.com")
                .isUnread(true)
                .build();
        conv.setId(convId);
        conv.setOrganizationId(platformOrgId);

        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("info@taxoryn.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_token_123")
                .build();
        account.setId(accountId);
        account.setOrganizationId(platformOrgId);

        when(conversationRepository.findByIdAndOrganizationId(convId, platformOrgId)).thenReturn(Optional.of(conv));
        when(gmailOAuthService.getValidAuthenticatedAccount(platformOrgId, accountId)).thenReturn(account);
        when(encryptionService.decrypt("enc_token_123")).thenReturn("plain_access_token");

        GmailThreadModels.MessageMetadata msgMeta = GmailThreadModels.MessageMetadata.builder()
                .id("msg_1")
                .threadId(threadId)
                .internalDate(1710000000000L)
                .snippet("Hello, I would like to schedule a demo.")
                .payload(GmailThreadModels.MessagePayload.builder()
                        .headers(List.of(
                                new GmailThreadModels.HeaderEntry("From", "Dr. Sharma <lead@practice.com>"),
                                new GmailThreadModels.HeaderEntry("To", "info@taxoryn.com"),
                                new GmailThreadModels.HeaderEntry("Subject", "Onboarding Demo Request")
                        ))
                        .body(GmailThreadModels.MessageBody.builder().data("").build())
                        .build())
                .build();

        GmailThreadModels.ThreadDetail threadDetail = GmailThreadModels.ThreadDetail.builder()
                .id(threadId)
                .messages(List.of(msgMeta))
                .build();

        when(apiClient.getThreadFull("plain_access_token", threadId)).thenReturn(threadDetail);

        List<GmailMessageViewDto> messageViews = conversationService.getConversationMessages(convId, adminScope);

        assertThat(messageViews).hasSize(1);
        assertThat(messageViews.get(0).getFrom()).isEqualTo("Dr. Sharma <lead@practice.com>");
        assertThat(messageViews.get(0).getSnippet()).isEqualTo("Hello, I would like to schedule a demo.");
        assertThat(conv.getIsUnread()).isFalse();
        verify(conversationRepository).save(conv);
    }

    @Test
    @DisplayName("Verify sendReply dispatches email via Gmail API and updates conversation to REPLIED")
    void testSendReplyDispatchesAndUpdatesStatus() {
        UUID convId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String threadId = "th_reply_44";

        GmailConversationEntity conv = GmailConversationEntity.builder()
                .gmailAccountId(accountId)
                .threadId(threadId)
                .subject("Support Request: GST filing error")
                .senderEmail("client@firm.com")
                .senderName("CA Rajesh")
                .status(GmailConversationStatus.OPEN)
                .messageCount(1)
                .isUnread(true)
                .build();
        conv.setId(convId);
        conv.setOrganizationId(platformOrgId);

        GmailAccountEntity account = GmailAccountEntity.builder()
                .emailAddress("support@taxoryn.com")
                .status(GmailAccountStatus.CONNECTED)
                .encryptedAccessToken("enc_token_456")
                .build();
        account.setId(accountId);
        account.setOrganizationId(platformOrgId);

        when(conversationRepository.findByIdAndOrganizationId(convId, platformOrgId)).thenReturn(Optional.of(conv));
        when(gmailOAuthService.getValidAuthenticatedAccount(platformOrgId, accountId)).thenReturn(account);
        when(encryptionService.decrypt("enc_token_456")).thenReturn("plain_access_token_456");
        when(conversationRepository.save(any(GmailConversationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GmailReplyRequest replyRequest = GmailReplyRequest.builder()
                .replyAccountId(accountId)
                .to("client@firm.com")
                .subject("Re: Support Request: GST filing error")
                .body("Hello Rajesh, we have resolved the issue with your JSON export.")
                .inReplyToMessageId("msg_parent_1")
                .build();

        GmailConversationDto result = conversationService.sendReply(convId, replyRequest, adminScope);

        assertThat(result.getStatus()).isEqualTo(GmailConversationStatus.REPLIED);
        assertThat(result.getMessageCount()).isEqualTo(2);
        assertThat(result.getFirstResponseAt()).isNotNull();
        assertThat(result.getResolvedAt()).isNotNull();

        verify(apiClient).sendMessage(eq("plain_access_token_456"), any(String.class), eq(threadId));
        verify(auditService).logEvent(eq(platformOrgId), eq(adminUserId), eq("GMAIL_REPLY_SENT"), eq("GMAIL_CONVERSATION"), eq(convId.toString()), eq("OPEN"), contains("support@taxoryn.com"));
    }
}
