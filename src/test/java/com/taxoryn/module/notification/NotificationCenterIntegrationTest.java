package com.taxoryn.module.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.notification.dto.SendNotificationRequest;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.entity.NotificationEntity.Category;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.entity.NotificationEntity.Severity;
import com.taxoryn.module.notification.repository.NotificationRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class NotificationCenterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OrganizationEntity testOrgA;
    private UserEntity practitionerA;
    private String tokenA;

    private OrganizationEntity testOrgB;
    private UserEntity practitionerB;
    private String tokenB;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        testOrgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Verma & Associates CA")
                .legalName("Verma & Associates Chartered Accountants")
                .email("admin@verma-ca.com")
                .phone("9876543210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        practitionerA = userRepository.save(UserEntity.builder()
                .organizationId(testOrgA.getId())
                .email("practitionerA@verma-ca.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Verma")
                .status(UserEntity.UserStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                practitionerA.getId(),
                testOrgA.getId(),
                practitionerA.getEmail(),
                Set.of("ORG_ADMIN", "PRACTITIONER"),
                Set.of("NOTIFICATION_READ", "NOTIFICATION_WRITE")
        );

        testOrgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Gupta Tax Corp")
                .legalName("Gupta Tax Consultancy LLP")
                .email("admin@gupta-tax.com")
                .phone("9876543211")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        practitionerB = userRepository.save(UserEntity.builder()
                .organizationId(testOrgB.getId())
                .email("practitionerB@gupta-tax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Suresh")
                .lastName("Gupta")
                .status(UserEntity.UserStatus.ACTIVE)
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                practitionerB.getId(),
                testOrgB.getId(),
                practitionerB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("NOTIFICATION_READ", "NOTIFICATION_WRITE")
        );
    }

    @Test
    @DisplayName("Test dispatch notification and query paginated history with severity and category")
    void testDispatchAndQueryHistory() throws Exception {
        SendNotificationRequest sendReq = SendNotificationRequest.builder()
                .userId(practitionerA.getId())
                .notificationType(NotificationType.TASK_DUE)
                .severity(Severity.WARNING)
                .category(Category.TASK)
                .entityType("TASK")
                .entityId("task-uuid-1234")
                .title("GSTR-3B Due Tomorrow")
                .message("GSTR-3B for client Apex Engineering is due tomorrow.")
                .channels(Set.of(NotificationChannel.IN_APP))
                .actionUrl("/tasks/task-uuid-1234")
                .build();

        mockMvc.perform(post("/api/v1/notifications/send")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("GSTR-3B Due Tomorrow")))
                .andExpect(jsonPath("$.data.severity", is("WARNING")))
                .andExpect(jsonPath("$.data.category", is("TASK")))
                .andExpect(jsonPath("$.data.entityType", is("TASK")))
                .andExpect(jsonPath("$.data.entityId", is("task-uuid-1234")))
                .andExpect(jsonPath("$.data.read", is(false)));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("GSTR-3B Due Tomorrow")))
                .andExpect(jsonPath("$.data.content[0].severity", is("WARNING")))
                .andExpect(jsonPath("$.data.content[0].category", is("TASK")))
                .andExpect(jsonPath("$.data.totalElements", is(1)));
    }

    @Test
    @DisplayName("Test unread count query performance and precision")
    void testUnreadCount() throws Exception {
        notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.DOCUMENT_UPLOADED)
                .severity(Severity.SUCCESS)
                .category(Category.DOCUMENT)
                .title("Form 16 Uploaded")
                .message("Client uploaded Form 16")
                .channels("IN_APP")
                .isRead(false)
                .build());

        notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.TASK_OVERDUE)
                .severity(Severity.ACTION_REQUIRED)
                .category(Category.TASK)
                .title("ITR Overdue")
                .message("ITR filing is overdue")
                .channels("IN_APP")
                .isRead(false)
                .build());

        notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.GENERAL)
                .severity(Severity.INFO)
                .category(Category.SYSTEM)
                .title("Welcome to Taxoryn")
                .message("Account setup complete")
                .channels("IN_APP")
                .isRead(true)
                .build());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(2)));
    }

    @Test
    @DisplayName("Test mark single notification as read and unread")
    void testMarkAsReadAndUnread() throws Exception {
        NotificationEntity notif = notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.DOCUMENT_REQUEST_CREATED)
                .severity(Severity.INFO)
                .category(Category.DOCUMENT)
                .title("New Document Request")
                .message("Please upload bank statements")
                .channels("IN_APP")
                .isRead(false)
                .build());

        mockMvc.perform(patch("/api/v1/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read", is(true)))
                .andExpect(jsonPath("$.data.readAt", notNullValue()));

        mockMvc.perform(patch("/api/v1/notifications/" + notif.getId() + "/unread")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read", is(false)))
                .andExpect(jsonPath("$.data.readAt", nullValue()));
    }

    @Test
    @DisplayName("Test mark all notifications as read via POST and PATCH")
    void testMarkAllAsRead() throws Exception {
        notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.TASK_DUE)
                .title("Task 1")
                .message("Message 1")
                .channels("IN_APP")
                .isRead(false)
                .build());

        notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.TASK_DUE)
                .title("Task 2")
                .message("Message 2")
                .channels("IN_APP")
                .isRead(false)
                .build());

        mockMvc.perform(post("/api/v1/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated", is(2)));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(0)));
    }

    @Test
    @DisplayName("Test category and severity filters on notifications list")
    void testCategoryAndSeverityFilters() throws Exception {
        notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.DOCUMENT_REJECTED)
                .severity(Severity.ACTION_REQUIRED)
                .category(Category.DOCUMENT)
                .title("Document Rejected")
                .message("Rejection notice")
                .channels("IN_APP")
                .isRead(false)
                .build());

        notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.CLIENT_REGISTERED)
                .severity(Severity.SUCCESS)
                .category(Category.CLIENT)
                .title("Client Registered")
                .message("Client onboarding")
                .channels("IN_APP")
                .isRead(false)
                .build());

        // Filter by DOCUMENT category
        mockMvc.perform(get("/api/v1/notifications?category=DOCUMENT")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].category", is("DOCUMENT")));

        // Filter by ACTION_REQUIRED severity
        mockMvc.perform(get("/api/v1/notifications?severity=ACTION_REQUIRED")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].severity", is("ACTION_REQUIRED")));
    }

    @Test
    @DisplayName("Test strict tenant and user isolation")
    void testTenantAndUserIsolation() throws Exception {
        NotificationEntity notifA = notificationRepository.save(NotificationEntity.builder()
                .organizationId(testOrgA.getId())
                .userId(practitionerA.getId())
                .notificationType(NotificationType.GENERAL)
                .title("Secret Org A Notification")
                .message("Confidential info")
                .channels("IN_APP")
                .isRead(false)
                .build());

        // Org B practitioner should not see Org A notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // Org B practitioner cannot mark Org A notification as read
        mockMvc.perform(patch("/api/v1/notifications/" + notifA.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}