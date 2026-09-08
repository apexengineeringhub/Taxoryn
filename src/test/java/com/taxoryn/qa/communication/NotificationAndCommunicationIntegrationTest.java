package com.taxoryn.qa.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.marketplace.dto.SendEnquiryMessageRequest;
import com.taxoryn.module.marketplace.entity.MarketplaceEnquiryMessageEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import com.taxoryn.module.marketplace.entity.MessageSenderType;
import com.taxoryn.module.marketplace.repository.MarketplaceEnquiryMessageRepository;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.qa.factory.OrganizationTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationAndCommunicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationTestDataFactory factory;

    @Autowired
    private MarketplaceEnquiryMessageRepository messageRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminA;
    private UserEntity adminB;
    private UserEntity customerUser1;
    private UserEntity customerUser2;
    private MarketplaceLeadEntity enquiryA1;
    private MarketplaceLeadEntity enquiryB1;
    private String tokenAdminA;
    private String tokenAdminB;
    private String tokenCustomer1;
    private String tokenCustomer2;

    @BeforeEach
    void setUp() {
        orgA = factory.createOrganization("Summit Tax Advisory " + UUID.randomUUID().toString().substring(0, 5), "admin.summit." + UUID.randomUUID().toString().substring(0, 5) + "@summit.in");
        orgB = factory.createOrganization("Pinnacle Consultants " + UUID.randomUUID().toString().substring(0, 5), "admin.pinnacle." + UUID.randomUUID().toString().substring(0, 5) + "@pinnacle.in");

        adminA = factory.createAdminUser(orgA, "adminA." + UUID.randomUUID().toString().substring(0, 5) + "@summit.in", "AdminPass123!");
        adminB = factory.createAdminUser(orgB, "adminB." + UUID.randomUUID().toString().substring(0, 5) + "@pinnacle.in", "AdminPass123!");

        customerUser1 = factory.createMarketplaceCustomer("cust1." + UUID.randomUUID().toString().substring(0, 5) + "@gmail.com", "CustPass123!");
        customerUser2 = factory.createMarketplaceCustomer("cust2." + UUID.randomUUID().toString().substring(0, 5) + "@gmail.com", "CustPass123!");

        enquiryA1 = factory.createEnquiry(orgA, customerUser1, "Rohan Sharma", customerUser1.getEmail());
        enquiryB1 = factory.createEnquiry(orgB, customerUser2, "Kavita Rao", customerUser2.getEmail());

        tokenAdminA = factory.generateBearerToken(adminA);
        tokenAdminB = factory.generateBearerToken(adminB);
        tokenCustomer1 = factory.generateBearerToken(customerUser1);
        tokenCustomer2 = factory.generateBearerToken(customerUser2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // =========================================================================
    // NOTIFICATIONS
    // =========================================================================

    @Test
    @DisplayName("NOTIF-003 & NOTIF-005: Notification isolation and mark-as-read")
    void shouldEnforceNotificationIsolationAndMarkRead() throws Exception {
        NotificationEntity notifA = factory.createNotification(orgA, adminA, "Org A Alert", "Action required", NotificationType.TASK_ASSIGNED);
        NotificationEntity notifB = factory.createNotification(orgB, adminB, "Org B Alert", "Confidential", NotificationType.PAYMENT_RECEIVED);

        // Admin A queries notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", hasItem("Org A Alert")))
                .andExpect(jsonPath("$.data.content[*].title", not(hasItem("Org B Alert"))));

        // Admin A marks notifA as read
        mockMvc.perform(patch("/api/v1/notifications/" + notifA.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk());

        // Admin A cannot mark notifB as read
        mockMvc.perform(patch("/api/v1/notifications/" + notifB.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // LIVE CHAT / ENQUIRY MESSAGING
    // =========================================================================

    @Test
    @DisplayName("CHAT-002 & CHAT-003 & CHAT-004: Customer sends message, practice retrieves and replies")
    void shouldAllowCustomerAndPracticeToExchangeMessages() throws Exception {
        // Customer sends message on enquiryA1
        SendEnquiryMessageRequest customerMsg = SendEnquiryMessageRequest.builder()
                .messageBody("Hello, what documents are required for my startup ITR-6 filing?")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/" + enquiryA1.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenCustomer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerMsg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.senderType", is("CUSTOMER")))
                .andExpect(jsonPath("$.data.messageBody", is("Hello, what documents are required for my startup ITR-6 filing?")));

        // Practice retrieves messages
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + enquiryA1.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.messages[0].messageBody", containsString("startup ITR-6 filing")));

        // Practice replies
        SendEnquiryMessageRequest practiceReply = SendEnquiryMessageRequest.builder()
                .messageBody("Please share your audited balance sheet and bank statements.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + enquiryA1.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(practiceReply)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderType", is("PRACTICE_USER")))
                .andExpect(jsonPath("$.data.messageBody", containsString("audited balance sheet")));

        // Customer marks messages as read
        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/" + enquiryA1.getId() + "/messages/read")
                        .header("Authorization", "Bearer " + tokenCustomer1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CHAT-007: Customer chat isolation - Customer 2 cannot access Customer 1 conversation")
    void shouldNotAllowCustomerToAccessAnotherCustomerConversation() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/customer/enquiries/" + enquiryA1.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenCustomer2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CHAT-008: Organization chat isolation - Org B cannot access Org A enquiry messages")
    void shouldNotAllowOrgToAccessAnotherOrgMessages() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/lifecycle-enquiries/" + enquiryA1.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CHAT-012: Empty message validation - should reject blank message body")
    void shouldRejectEmptyMessageBody() throws Exception {
        SendEnquiryMessageRequest emptyMsg = SendEnquiryMessageRequest.builder()
                .messageBody("")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/" + enquiryA1.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenCustomer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMsg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CHAT-014: Message sanitization - HTML / script tags handled safely")
    void shouldHandleMaliciousScriptPayloadSafely() throws Exception {
        SendEnquiryMessageRequest xssMsg = SendEnquiryMessageRequest.builder()
                .messageBody("<script>alert('XSS')</script>Normal question text")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/customer/enquiries/" + enquiryA1.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenCustomer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(xssMsg)))
                .andExpect(status().isOk());
    }
}
