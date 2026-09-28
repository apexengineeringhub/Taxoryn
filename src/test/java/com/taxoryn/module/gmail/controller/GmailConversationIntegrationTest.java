package com.taxoryn.module.gmail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.dto.GmailAccountConnectRequest;
import com.taxoryn.module.gmail.dto.GmailConversationUpdateDto;
import com.taxoryn.module.gmail.dto.GmailLinkClientRequest;
import com.taxoryn.module.gmail.dto.GmailThreadModels;
import com.taxoryn.module.gmail.dto.GoogleOAuthTokenResponse;
import com.taxoryn.module.gmail.dto.GoogleUserInfoResponse;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
import com.taxoryn.module.gmail.entity.GmailConversationEntity;
import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.repository.GmailSyncHistoryRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GmailConversationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private GmailAccountRepository accountRepository;

    @Autowired
    private GmailConversationRepository conversationRepository;

    @Autowired
    private GmailSyncHistoryRepository syncHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private com.taxoryn.module.gmail.util.TokenEncryptionService tokenEncryptionService;

    @MockBean
    private GmailApiClient mockApiClient;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private UserEntity staffUser1;
    private UserEntity adminUser2;
    private String adminToken1;
    private String staffToken1;
    private String adminToken2;
    private ClientEntity client1;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        conversationRepository.deleteAll();
        syncHistoryRepository.deleteAll();
        accountRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organizations
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
                .email("admin@apextax.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Tax Advisory")
                .email("admin@globaltax.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity practitionerRole = roleRepository.save(RoleEntity.builder()
                .code("PRACTITIONER")
                .name("Practitioner")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // Org 1 Users
        TenantContext.setTenantId(org1.getId());

        adminUser1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("admin@apextax.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Rajesh")
                .lastName("Verma")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build());

        staffUser1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("staff@apextax.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("Amit")
                .lastName("Sharma")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(practitionerRole))
                .build());

        client1 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Traders")
                .email("contact@alphatraders.com")
                .pan("ABCDE1234F")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        // Org 2 Users
        TenantContext.setTenantId(org2.getId());

        adminUser2 = userRepository.save(UserEntity.builder()
                .organizationId(org2.getId())
                .email("admin@globaltax.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .firstName("John")
                .lastName("Doe")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build());

        // Tokens
        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("GMAIL_VIEW", "GMAIL_MANAGE")
        );

        staffToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                staffUser1.getId(),
                org1.getId(),
                staffUser1.getEmail(),
                Set.of("PRACTITIONER"),
                Set.of("GMAIL_VIEW")
        );

        adminToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(),
                org2.getId(),
                adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("GMAIL_VIEW", "GMAIL_MANAGE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("E2E: Generate authorization URL")
    void testGetAuthorizationUrl() throws Exception {
        mockMvc.perform(get("/api/v1/gmail/accounts/authorize-url")
                        .header("Authorization", adminToken1)
                        .param("redirectUri", "http://localhost:5173/auth/callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.authorizationUrl").isNotEmpty());
    }

    @Test
    @DisplayName("E2E: Connect Gmail account via OAuth code exchange")
    void testConnectGmailAccount() throws Exception {
        GoogleOAuthTokenResponse tokenResponse = GoogleOAuthTokenResponse.builder()
                .accessToken("ya29.sample_access_token")
                .refreshToken("1//sample_refresh_token")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .build();

        GoogleUserInfoResponse userInfo = GoogleUserInfoResponse.builder()
                .email("mailbox@apextax.com")
                .name("Apex Mailbox")
                .build();

        when(mockApiClient.exchangeAuthCode(eq("auth_code_123"), any())).thenReturn(tokenResponse);
        when(mockApiClient.getUserInfo("ya29.sample_access_token")).thenReturn(userInfo);

        GmailAccountConnectRequest request = GmailAccountConnectRequest.builder()
                .authCode("auth_code_123")
                .redirectUri("http://localhost:5173/auth/callback")
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .build();

        mockMvc.perform(post("/api/v1/gmail/accounts/connect")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.emailAddress", is("mailbox@apextax.com")))
                .andExpect(jsonPath("$.data.status", is("CONNECTED")))
                .andExpect(jsonPath("$.data.accountType", is("PRACTICE_SHARED")));
    }

    @Test
    @DisplayName("E2E: List connected accounts and trigger on-demand sync")
    void testListAccountsAndTriggerSync() throws Exception {
        TenantContext.setTenantId(org1.getId());
        GmailAccountEntity account = accountRepository.save(GmailAccountEntity.builder()
                .emailAddress("practice@apextax.com")
                .status(GmailAccountStatus.CONNECTED)
                .accountType(GmailAccountType.PRACTICE_SHARED)
                .encryptedAccessToken(tokenEncryptionService.encrypt("ya29.sample_valid_token"))
                .tokenExpiresAt(Instant.now().plus(1, java.time.temporal.ChronoUnit.HOURS))
                .build());
        TenantContext.clear();

        // 1. List accounts
        mockMvc.perform(get("/api/v1/gmail/accounts")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].emailAddress", is("practice@apextax.com")));

        // 2. Trigger sync with mocked thread list and detail
        when(mockApiClient.listThreads(any(), any(), any(), eq(50)))
                .thenReturn(GmailThreadModels.ThreadListResponse.builder().threads(List.of()).build());

        mockMvc.perform(post("/api/v1/gmail/accounts/" + account.getId() + "/sync")
                        .header("Authorization", adminToken1)
                        .param("syncType", "FULL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SUCCESS")));
    }

    @Test
    @DisplayName("E2E: Conversation search, details, status update, assignment, and client linking")
    void testConversationFullLifecycle() throws Exception {
        TenantContext.setTenantId(org1.getId());
        GmailAccountEntity account = accountRepository.save(GmailAccountEntity.builder()
                .emailAddress("practice@apextax.com")
                .status(GmailAccountStatus.CONNECTED)
                .build());

        GmailConversationEntity conv = conversationRepository.save(GmailConversationEntity.builder()
                .gmailAccountId(account.getId())
                .threadId("thread_9911")
                .subject("GST Query on Inward Supplies")
                .snippet("Kindly clarify the ITC eligibility for capital goods...")
                .senderEmail("client@corp.com")
                .senderName("Corporate Client")
                .status(GmailConversationStatus.OPEN)
                .priority(GmailConversationPriority.NORMAL)
                .messageCount(1)
                .isUnread(true)
                .build());
        TenantContext.clear();

        // 1. Search conversations
        mockMvc.perform(get("/api/v1/gmail/conversations")
                        .header("Authorization", adminToken1)
                        .param("search", "Inward Supplies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].subject", is("GST Query on Inward Supplies")));

        // 2. Get conversation by ID
        mockMvc.perform(get("/api/v1/gmail/conversations/" + conv.getId())
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.threadId", is("thread_9911")));

        // 3. Update conversation status to IN_PROGRESS and priority to HIGH
        GmailConversationUpdateDto updateDto = GmailConversationUpdateDto.builder()
                .status(GmailConversationStatus.IN_PROGRESS)
                .priority(GmailConversationPriority.HIGH)
                .build();

        mockMvc.perform(patch("/api/v1/gmail/conversations/" + conv.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.data.priority", is("HIGH")));

        // 4. Assign conversation to staff
        mockMvc.perform(post("/api/v1/gmail/conversations/" + conv.getId() + "/assign")
                        .header("Authorization", adminToken1)
                        .param("userId", staffUser1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedUserId", is(staffUser1.getId().toString())));

        // 5. Link client
        GmailLinkClientRequest linkRequest = GmailLinkClientRequest.builder()
                .clientId(client1.getId())
                .build();

        mockMvc.perform(post("/api/v1/gmail/conversations/" + conv.getId() + "/link-client")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId", is(client1.getId().toString())))
                .andExpect(jsonPath("$.data.clientDisplayName", is("Alpha Traders")));

        // 6. Check operational metrics
        mockMvc.perform(get("/api/v1/gmail/conversations/metrics")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalConversations", is(1)))
                .andExpect(jsonPath("$.data.inProgressConversations", is(1)));

        // 7. Unlink client
        mockMvc.perform(delete("/api/v1/gmail/conversations/" + conv.getId() + "/link-client")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").doesNotExist());
    }

    @Test
    @DisplayName("E2E: Enforce strict multi-tenant isolation (Cross-tenant access rejected)")
    void testMultiTenantIsolationForbidden() throws Exception {
        TenantContext.setTenantId(org1.getId());
        GmailAccountEntity account1 = accountRepository.save(GmailAccountEntity.builder()
                .emailAddress("org1@apextax.com")
                .status(GmailAccountStatus.CONNECTED)
                .build());

        GmailConversationEntity conv1 = conversationRepository.save(GmailConversationEntity.builder()
                .gmailAccountId(account1.getId())
                .threadId("th_org1_private")
                .subject("Confidential Tax Advisory")
                .status(GmailConversationStatus.OPEN)
                .build());
        TenantContext.clear();

        // Org 2 user attempting to access Org 1 conversation -> 404 (Resource not found for this tenant)
        mockMvc.perform(get("/api/v1/gmail/conversations/" + conv1.getId())
                        .header("Authorization", adminToken2))
                .andExpect(status().isNotFound());

        // Org 2 user attempting to access Org 1 account -> 404
        mockMvc.perform(get("/api/v1/gmail/accounts/" + account1.getId())
                        .header("Authorization", adminToken2))
                .andExpect(status().isNotFound());
    }
}
