package com.taxoryn.qa.factory;

import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentScanStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.entity.DocumentEntity.StorageProvider;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.marketplace.entity.EnquiryStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceEnquiryMessageEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MessageSenderType;
import com.taxoryn.module.marketplace.repository.MarketplaceEnquiryMessageRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.repository.NotificationRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationTestDataFactory {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestItemRepository documentRequestItemRepository;
    private final DocumentRepository documentRepository;
    private final MarketplaceProfileRepository marketplaceProfileRepository;
    private final MarketplaceLeadRepository marketplaceLeadRepository;
    private final MarketplaceEnquiryMessageRepository marketplaceEnquiryMessageRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public static final String DEFAULT_TEST_PASSWORD = "ValidStrongPass123!";

    @Transactional
    public OrganizationEntity createOrganization(String name, String email) {
        OrganizationEntity org = OrganizationEntity.builder()
                .name(name)
                .email(email.toLowerCase().trim())
                .phone("+919876543210")
                .pan("AAACT" + (int)(Math.random() * 8999 + 1000) + "A")
                .gstin("27AAACT" + (int)(Math.random() * 8999 + 1000) + "A1Z5")
                .status(OrganizationStatus.ACTIVE)
                .subscriptionPlan(SubscriptionPlan.STARTER)
                .build();
        return organizationRepository.save(org);
    }

    @Transactional
    public UserEntity createAdminUser(OrganizationEntity org, String email, String password) {
        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password != null ? password : DEFAULT_TEST_PASSWORD))
                .firstName("Admin")
                .lastName(org.getName())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        user.setOrganizationId(org.getId());
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity createEmployeeUser(OrganizationEntity org, String email, String roleCode, String password) {
        RoleEntity role = roleRepository.findByCodeAndIsSystemRoleTrue(roleCode)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code(roleCode)
                        .name(roleCode)
                        .isSystemRole(true)
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password != null ? password : DEFAULT_TEST_PASSWORD))
                .firstName("Staff")
                .lastName(roleCode)
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        user.setOrganizationId(org.getId());
        return userRepository.save(user);
    }

    @Transactional
    public EmployeeEntity createEmployee(OrganizationEntity org, UserEntity user, String employeeCode, String department, String designation) {
        EmployeeEntity employee = EmployeeEntity.builder()
                .userId(user.getId())
                .employeeCode(employeeCode)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone("+919876543211")
                .department(department != null ? department : "Tax")
                .designation(designation != null ? designation : "Tax Consultant")
                .joiningDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee.setOrganizationId(org.getId());
        return employeeRepository.save(employee);
    }

    @Transactional
    public ClientEntity createClient(OrganizationEntity org, String displayName, String email) {
        ClientEntity client = ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .displayName(displayName)
                .legalName(displayName)
                .email(email.toLowerCase().trim())
                .phone("+919876543212")
                .pan("ABCDE" + (int)(Math.random() * 8999 + 1000) + "F")
                .status(ClientStatus.ACTIVE)
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .build();
        client.setOrganizationId(org.getId());
        return clientRepository.save(client);
    }

    @Transactional
    public UserEntity createClientPortalUser(OrganizationEntity org, ClientEntity client, String email, String password) {
        RoleEntity clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_ADMIN")
                        .name("Client Portal Administrator")
                        .isSystemRole(true)
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password != null ? password : DEFAULT_TEST_PASSWORD))
                .firstName(client.getDisplayName())
                .lastName("ClientUser")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build();
        user.setOrganizationId(org.getId());
        user.setClientId(client.getId());
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity createMarketplaceCustomer(String email, String password) {
        RoleEntity customerRole = roleRepository.findByCodeAndIsSystemRoleTrue("MARKETPLACE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("MARKETPLACE_CUSTOMER")
                        .name("Marketplace Customer")
                        .isSystemRole(true)
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password != null ? password : DEFAULT_TEST_PASSWORD))
                .firstName("Marketplace")
                .lastName("Customer")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build();
        user.setOrganizationId(null);
        return userRepository.save(user);
    }

    @Transactional
    public MarketplaceProfileEntity getOrCreateMarketplaceProfile(OrganizationEntity org) {
        return marketplaceProfileRepository.findByOrganizationId(org.getId())
                .orElseGet(() -> {
                    MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                            .organizationId(org.getId())
                            .displayName(org.getName())
                            .slug("slug-" + org.getId().toString().substring(0, 8))
                            .headline("Expert Tax Practice")
                            .bio("Professional Chartered Accountants")
                            .city("Mumbai")
                            .state("Maharashtra")
                            .isPublished(true)
                            .verificationStatus(VerificationStatus.VERIFIED)
                            .build();
                    return marketplaceProfileRepository.save(profile);
                });
    }

    @Transactional
    public MarketplaceLeadEntity createEnquiry(OrganizationEntity org, UserEntity customer, String clientName, String email) {
        MarketplaceProfileEntity profile = getOrCreateMarketplaceProfile(org);
        MarketplaceLeadEntity lead = MarketplaceLeadEntity.builder()
                .organizationId(org.getId())
                .marketplaceProfileId(profile.getId())
                .customerId(customer != null ? customer.getId() : null)
                .clientName(clientName)
                .clientEmail(email)
                .clientPhone("+919876543213")
                .enquiryStatus(EnquiryStatus.NEW)
                .earlyEnquiryMessage("Need help with tax planning")
                .build();
        return marketplaceLeadRepository.save(lead);
    }

    @Transactional
    public MarketplaceEnquiryMessageEntity createEnquiryMessage(UUID enquiryId, MessageSenderType senderType, UUID senderUserId, String senderName, String messageBody) {
        MarketplaceEnquiryMessageEntity msg = MarketplaceEnquiryMessageEntity.builder()
                .enquiryId(enquiryId)
                .senderType(senderType)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .messageBody(messageBody)
                .isReadByCustomer(senderType == MessageSenderType.CUSTOMER)
                .isReadByPractice(senderType == MessageSenderType.PRACTICE_USER)
                .build();
        return marketplaceEnquiryMessageRepository.save(msg);
    }

    @Transactional
    public DocumentRequestEntity createDocumentRequest(OrganizationEntity org, ClientEntity client, UserEntity creator, String purpose, List<String> itemTitles) {
        DocumentRequestEntity request = DocumentRequestEntity.builder()
                .clientId(client.getId())
                .requestedByUserId(creator != null ? creator.getId() : null)
                .requestNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8))
                .purpose(purpose)
                .message("Please upload your tax documents")
                .status(RequestStatus.SENT)
                .dueDate(LocalDate.now().plusDays(15))
                .build();
        request.setOrganizationId(org.getId());
        DocumentRequestEntity saved = documentRequestRepository.save(request);

        if (itemTitles != null && !itemTitles.isEmpty()) {
            for (int i = 0; i < itemTitles.size(); i++) {
                DocumentRequestItemEntity item = DocumentRequestItemEntity.builder()
                        .request(saved)
                        .clientId(client.getId())
                        .title(itemTitles.get(i))
                        .description("Item " + (i + 1))
                        .documentType(DocumentType.FORM_16)
                        .required(true)
                        .status(ItemStatus.PENDING)
                        .build();
                item.setOrganizationId(org.getId());
                documentRequestItemRepository.save(item);
            }
        }
        return saved;
    }

    private final com.taxoryn.module.document.storage.DocumentStorageService storageService;

    @Transactional
    public DocumentEntity createDocument(OrganizationEntity org, ClientEntity client, UserEntity uploader, String fileName, DocumentType docType) {
        byte[] mockBytes = "%PDF-1.4 Mock Test Tax Document Content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String storageKey = storageService.store(org.getId(), fileName, "application/pdf", mockBytes);

        DocumentEntity doc = DocumentEntity.builder()
                .clientId(client.getId())
                .fileName(fileName)
                .storageKey(storageKey)
                .storageProvider(StorageProvider.LOCAL)
                .fileSize((long) mockBytes.length)
                .contentType("application/pdf")
                .documentType(docType != null ? docType : DocumentType.FORM_16)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.CLEAN)
                .scannedAt(java.time.Instant.now())
                .scannerName("Test-Mock-Scanner")
                .build();
        doc.setOrganizationId(org.getId());
        return documentRepository.save(doc);
    }

    @Transactional
    public NotificationEntity createNotification(OrganizationEntity org, UserEntity user, String title, String message, NotificationType type) {
        NotificationEntity notif = NotificationEntity.builder()
                .organizationId(org != null ? org.getId() : UUID.randomUUID())
                .userId(user.getId())
                .title(title)
                .message(message)
                .notificationType(type != null ? type : NotificationType.TASK_ASSIGNED)
                .category(NotificationEntity.Category.SYSTEM)
                .severity(NotificationEntity.Severity.INFO)
                .isRead(false)
                .channels("IN_APP")
                .build();
        return notificationRepository.save(notif);
    }

    public String generateBearerToken(UserEntity user) {
        Set<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toSet())
                : Set.of();
        Set<String> permissions = new HashSet<>();
        if (user.getRoles() != null) {
            for (RoleEntity r : user.getRoles()) {
                if (r.getPermissions() != null) {
                    permissions.addAll(r.getPermissions().stream().map(p -> p.getCode()).toList());
                }
            }
        }
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getOrganizationId(), user.getClientId(), user.getEmail(), roles, permissions);
    }
}
