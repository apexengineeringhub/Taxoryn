package com.taxoryn.qa.security;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.core.security.bootstrap.ProductionSecurityValidator;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientServiceImpl;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notification.email.config.EmailProperties;
import com.taxoryn.module.notification.whatsapp.config.WhatsAppProperties;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientAndUrlSecurityIntegrationTest {

    // =========================================================================
    // PART 1: PRODUCTION LOGIN URL CANONICALIZATION & REJECTION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Part 1: Production Login & Portal URL Canonicalization Tests")
    class ProductionUrlTests {

        @Mock
        private Environment environment;
        @Mock
        private UserRepository userRepository;
        @Mock
        private PasswordEncoder passwordEncoder;

        private ProductionSecurityValidator createValidator() {
            return new ProductionSecurityValidator(environment, userRepository, passwordEncoder);
        }

        private void configureValidProductionBasics(ProductionSecurityValidator validator) {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            ReflectionTestUtils.setField(validator, "datasourceUrl", "jdbc:postgresql://db.taxoryn.internal:5432/taxoryn_prod");
            ReflectionTestUtils.setField(validator, "datasourceUsername", "taxoryn_prod_app");
            ReflectionTestUtils.setField(validator, "datasourcePassword", "Tx9#SecureP@ss2026!ProdDb");
            ReflectionTestUtils.setField(validator, "jwtSecret", "c3VwZXJzZWNyZXRwcm9kdWN0aW9ua2V5MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=");
            ReflectionTestUtils.setField(validator, "demoEnabled", false);
            ReflectionTestUtils.setField(validator, "storageProvider", "S3");
            ReflectionTestUtils.setField(validator, "storageS3Bucket", "taxoryn-production-docs");
            ReflectionTestUtils.setField(validator, "storageS3AccessKey", "AKIAIOSFODNN7EXAMPLE");
            ReflectionTestUtils.setField(validator, "storageS3SecretKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            ReflectionTestUtils.setField(validator, "mailEnabled", false);
            ReflectionTestUtils.setField(validator, "mailDevMode", false);
            ReflectionTestUtils.setField(validator, "whatsappEnabled", false);
            ReflectionTestUtils.setField(validator, "frontendUrl", "https://app.taxoryn.com");
            ReflectionTestUtils.setField(validator, "loginUrl", "https://app.taxoryn.com/login");
            ReflectionTestUtils.setField(validator, "activationUrl", "https://app.taxoryn.com/activate");
            ReflectionTestUtils.setField(validator, "resetPasswordUrl", "https://app.taxoryn.com/reset-password");
            ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.taxoryn.com,https://taxoryn.com");
            ReflectionTestUtils.setField(validator, "hibernateDdlAuto", "validate");
            ReflectionTestUtils.setField(validator, "flywayValidateOnMigrate", true);
            ReflectionTestUtils.setField(validator, "flywayEnabled", true);
            ReflectionTestUtils.setField(validator, "springdocApiDocsEnabled", false);
            ReflectionTestUtils.setField(validator, "springdocSwaggerUiEnabled", false);
        }

        @Test
        @DisplayName("EmailProperties canonicalizes apex domain taxoryn.com to app.taxoryn.com")
        void testEmailPropertiesCanonicalizesApexDomain() {
            EmailProperties props = new EmailProperties();
            props.setFrontendUrl("https://taxoryn.com");
            props.setLoginUrl("https://taxoryn.com/login");
            props.setActivationUrl("https://taxoryn.com/activate");
            props.setResetPasswordUrl("https://taxoryn.com/reset-password");

            assertEquals(EmailProperties.CANONICAL_PRODUCTION_FRONTEND_URL, props.getFrontendUrl());
            assertEquals(EmailProperties.CANONICAL_PRODUCTION_LOGIN_URL, props.getLoginUrl());
            assertEquals(EmailProperties.CANONICAL_PRODUCTION_ACTIVATION_URL, props.getActivationUrl());
            assertEquals(EmailProperties.CANONICAL_PRODUCTION_RESET_PASSWORD_URL, props.getResetPasswordUrl());
        }

        @Test
        @DisplayName("EmailProperties canonicalizes tenant subdomains to app.taxoryn.com")
        void testEmailPropertiesCanonicalizesTenantSubdomains() {
            EmailProperties props = new EmailProperties();
            props.setFrontendUrl("https://firm.taxoryn.com");
            props.setLoginUrl("https://firm.taxoryn.com/login");
            props.setActivationUrl("https://firm.taxoryn.com/activate");

            assertEquals(EmailProperties.CANONICAL_PRODUCTION_FRONTEND_URL, props.getFrontendUrl());
            assertEquals(EmailProperties.CANONICAL_PRODUCTION_LOGIN_URL, props.getLoginUrl());
            assertEquals(EmailProperties.CANONICAL_PRODUCTION_ACTIVATION_URL, props.getActivationUrl());
        }

        @Test
        @DisplayName("EmailProperties preserves localhost and development URLs")
        void testEmailPropertiesPreservesLocalhost() {
            EmailProperties props = new EmailProperties();
            props.setFrontendUrl("http://localhost:5173");
            props.setLoginUrl("http://localhost:5173/login");
            props.setActivationUrl("http://localhost:5173/activate");
            props.setResetPasswordUrl("http://localhost:5173/reset-password");

            assertEquals("http://localhost:5173", props.getFrontendUrl());
            assertEquals("http://localhost:5173/login", props.getLoginUrl());
            assertEquals("http://localhost:5173/activate", props.getActivationUrl());
            assertEquals("http://localhost:5173/reset-password", props.getResetPasswordUrl());
        }

        @Test
        @DisplayName("WhatsAppProperties canonicalizes apex domain and tenant subdomains to app.taxoryn.com/login")
        void testWhatsAppPropertiesCanonicalizesLoginUrl() {
            WhatsAppProperties props = new WhatsAppProperties();
            props.setLoginUrl("https://taxoryn.com/login");
            assertEquals(WhatsAppProperties.CANONICAL_PRODUCTION_LOGIN_URL, props.getLoginUrl());

            props.setLoginUrl("https://practice.taxoryn.com/login");
            assertEquals(WhatsAppProperties.CANONICAL_PRODUCTION_LOGIN_URL, props.getLoginUrl());

            props.setLoginUrl("https://app.taxoryn.com/login");
            assertEquals("https://app.taxoryn.com/login", props.getLoginUrl());
        }

        @Test
        @DisplayName("ProductionSecurityValidator rejects apex domain taxoryn.com as frontendUrl")
        void testValidatorRejectsApexDomainFrontendUrl() {
            ProductionSecurityValidator validator = createValidator();
            configureValidProductionBasics(validator);
            ReflectionTestUtils.setField(validator, "frontendUrl", "https://taxoryn.com");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
            assertTrue(ex.getMessage().contains("Production frontend URL must be exactly 'https://app.taxoryn.com'"));
        }

        @Test
        @DisplayName("ProductionSecurityValidator rejects tenant subdomain as frontendUrl")
        void testValidatorRejectsTenantSubdomainFrontendUrl() {
            ProductionSecurityValidator validator = createValidator();
            configureValidProductionBasics(validator);
            ReflectionTestUtils.setField(validator, "frontendUrl", "https://firm1.taxoryn.com");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
            assertTrue(ex.getMessage().contains("The marketing site (taxoryn.com) and any tenant subdomain"));
        }

        @Test
        @DisplayName("ProductionSecurityValidator rejects vercel demo domain in production")
        void testValidatorRejectsVercelDemoDomain() {
            ProductionSecurityValidator validator = createValidator();
            configureValidProductionBasics(validator);
            ReflectionTestUtils.setField(validator, "frontendUrl", "https://taxoryn-7x7f.vercel.app");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
            assertTrue(ex.getMessage().contains("cannot use demo Vercel domain"));
        }

        @Test
        @DisplayName("ProductionSecurityValidator rejects stale non-canonical loginUrl override")
        void testValidatorRejectsStaleLoginUrlOverride() {
            ProductionSecurityValidator validator = createValidator();
            configureValidProductionBasics(validator);
            ReflectionTestUtils.setField(validator, "loginUrl", "https://taxoryn.com/login");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
            assertTrue(ex.getMessage().contains("Production application URL (TAXORYN_LOGIN_URL / taxoryn.frontend.login-url)"));
            assertTrue(ex.getMessage().contains("must be exactly 'https://app.taxoryn.com/login'"));
        }

        @Test
        @DisplayName("ProductionSecurityValidator accepts exact canonical production configuration")
        void testValidatorAcceptsCanonicalProductionConfig() {
            ProductionSecurityValidator validator = createValidator();
            configureValidProductionBasics(validator);
            assertDoesNotThrow(validator::validateEnvironmentSecurity);
        }
    }

    // =========================================================================
    // PART 2: GLOBAL CLIENT TENANT & PRACTITIONER ISOLATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Part 2: Global Client Tenant and Practitioner Isolation Tests")
    class MultiTenantIsolationTests {

        @Mock
        private ClientRepository clientRepository;
        @Mock
        private EmployeeRepository employeeRepository;
        @Mock
        private TaskRepository taskRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;
        @Mock
        private com.taxoryn.module.client.mapper.ClientMapper clientMapper;
        @Mock
        private com.taxoryn.module.task.mapper.TaskMapper taskMapper;
        @Mock
        private com.taxoryn.module.audit.service.AuditService auditService;
        @Mock
        private com.taxoryn.module.notice.repository.TaxNoticeRepository noticeRepository;

        private PracticeSecurityScopeEvaluator securityScopeEvaluator;
        private ClientServiceImpl clientService;

        private final UUID orgA = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private final UUID orgB = UUID.fromString("22222222-2222-2222-2222-222222222222");

        private final UUID userA1Id = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
        private final UUID empA1Id = UUID.fromString("aaaaaaaa-1111-1111-1111-222222222222");
        private final UUID clientA1Id = UUID.fromString("aaaaaaaa-1111-1111-1111-333333333333");

        private final UUID userA2Id = UUID.fromString("aaaaaaaa-2222-2222-2222-111111111111");
        private final UUID empA2Id = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
        private final UUID clientA2Id = UUID.fromString("aaaaaaaa-2222-2222-2222-333333333333");

        private final UUID userB1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
        private final UUID empB1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-222222222222");
        private final UUID clientB1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-333333333333");

        private EmployeeEntity empA1;
        private EmployeeEntity empA2;
        private EmployeeEntity empB1;

        private ClientEntity clientA1;
        private ClientEntity clientA2;
        private ClientEntity clientB1;

        @BeforeEach
        void setUp() {
            securityScopeEvaluator = new PracticeSecurityScopeEvaluator(employeeRepository, clientRepository, taskRepository);
            clientService = new ClientServiceImpl(
                    clientRepository,
                    mock(com.taxoryn.module.client.repository.ClientNoteRepository.class),
                    employeeRepository,
                    taskRepository,
                    userRepository,
                    mock(com.taxoryn.module.role.repository.RoleRepository.class),
                    mock(com.taxoryn.module.organization.repository.OrganizationRepository.class),
                    mock(com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository.class),
                    mock(com.taxoryn.module.authentication.repository.RefreshTokenRepository.class),
                    mock(com.taxoryn.module.notification.email.service.EmailNotificationService.class),
                    subscriptionService,
                    securityScopeEvaluator,
                    noticeRepository,
                    clientMapper,
                    taskMapper,
                    auditService
            );

            empA1 = EmployeeEntity.builder()
                    .userId(userA1Id)
                    .employeeCode("EMP-A1")
                    .firstName("Practitioner")
                    .lastName("A1")
                    .email("a1@orga.com")
                    .department("Direct Tax")
                    .build();
            empA1.setId(empA1Id);
            empA1.setOrganizationId(orgA);

            empA2 = EmployeeEntity.builder()
                    .userId(userA2Id)
                    .employeeCode("EMP-A2")
                    .firstName("Practitioner")
                    .lastName("A2")
                    .email("a2@orga.com")
                    .department("Direct Tax")
                    .build();
            empA2.setId(empA2Id);
            empA2.setOrganizationId(orgA);

            empB1 = EmployeeEntity.builder()
                    .userId(userB1Id)
                    .employeeCode("EMP-B1")
                    .firstName("Practitioner")
                    .lastName("B1")
                    .email("b1@orgb.com")
                    .department("Audit")
                    .build();
            empB1.setId(empB1Id);
            empB1.setOrganizationId(orgB);

            clientA1 = ClientEntity.builder()
                    .displayName("Client A1 Alpha")
                    .pan("ABCDE1234F")
                    .assignedEmployeeId(empA1Id)
                    .status(ClientEntity.ClientStatus.ACTIVE)
                    .build();
            clientA1.setId(clientA1Id);
            clientA1.setOrganizationId(orgA);

            clientA2 = ClientEntity.builder()
                    .displayName("Client A2 Beta")
                    .pan("FGHIJ5678K")
                    .assignedEmployeeId(empA2Id)
                    .status(ClientEntity.ClientStatus.ACTIVE)
                    .build();
            clientA2.setId(clientA2Id);
            clientA2.setOrganizationId(orgA);

            clientB1 = ClientEntity.builder()
                    .displayName("Client B1 Gamma")
                    .pan("KLMNO9012P")
                    .assignedEmployeeId(empB1Id)
                    .status(ClientEntity.ClientStatus.ACTIVE)
                    .build();
            clientB1.setId(clientB1Id);
            clientB1.setOrganizationId(orgB);

            when(clientMapper.toDto(any(ClientEntity.class))).thenAnswer(inv -> {
                ClientEntity c = inv.getArgument(0);
                return ClientDto.builder()
                        .id(c.getId())
                        .displayName(c.getDisplayName())
                        .pan(c.getPan())
                        .assignedEmployeeId(c.getAssignedEmployeeId())
                        .status(c.getStatus())
                        .build();
            });
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }

        private void authenticateAs(UUID userId, UUID orgId, String email, Set<String> roles) {
            SecurityUser principal = SecurityUser.builder()
                    .userId(userId)
                    .organizationId(orgId)
                    .email(email)
                    .roles(roles)
                    .permissions(Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE"))
                    .enabled(true)
                    .build();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            TenantContext.setTenantId(orgId);
        }

        @Test
        @DisplayName("Practitioner A1 sees ONLY Client A1, cannot access Client A2 or Client B1")
        void testPractitionerIsolationWithinTenant() {
            authenticateAs(userA1Id, orgA, "a1@orga.com", Set.of("PRACTITIONER", "ROLE_PRACTITIONER"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, userA1Id)).thenReturn(Optional.of(empA1));
            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));
            when(clientRepository.findByIdAndOrganizationId(clientB1Id, orgA)).thenReturn(Optional.empty());

            when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                    .thenReturn(List.of(clientA1Id));
            when(clientMapper.toDto(any(ClientEntity.class))).thenAnswer(inv -> {
                ClientEntity c = inv.getArgument(0);
                return ClientDto.builder().id(c.getId()).displayName(c.getDisplayName()).build();
            });

            // 1. Access assigned client A1 -> SUCCESS
            ClientDto resultA1 = clientService.getClientById(clientA1Id);
            assertNotNull(resultA1);
            assertEquals(clientA1Id, resultA1.getId());

            // 2. Access unassigned client A2 in same tenant -> ACCESS DENIED
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientA2Id));

            // 3. Access client B1 from other tenant -> RESOURCE NOT FOUND (cross-tenant boundary)
            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientB1Id));
        }

        @Test
        @DisplayName("Practitioner with reportees does NOT gain department-wide visibility without explicit MANAGER role")
        void testPractitionerWithReporteesRemainsIndividualScoped() {
            // A1 is configured as manager of A2 in hierarchy, but role is PRACTITIONER (no MANAGER role)
            authenticateAs(userA1Id, orgA, "a1@orga.com", Set.of("PRACTITIONER", "ROLE_PRACTITIONER"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, userA1Id)).thenReturn(Optional.of(empA1));
            when(clientRepository.findIdsByOrganizationIdAndAssignedEmployeeIdIn(eq(orgA), any()))
                    .thenReturn(List.of(clientA1Id));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

            assertEquals(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL, scope.getRoleTier());
            assertFalse(scope.isDepartmentManager());
            assertFalse(scope.isFirmAdmin());
            assertTrue(scope.isStaff());

            // Accessible assignee IDs contain only self
            assertTrue(scope.getAccessibleAssigneeIds().contains(userA1Id));
            assertTrue(scope.getAccessibleAssigneeIds().contains(empA1Id));
            assertFalse(scope.getAccessibleAssigneeIds().contains(empA2Id));
        }

        @Test
        @DisplayName("Explicit ROLE_MANAGER grants department-wide visibility")
        void testRoleManagerGrantsDepartmentVisibility() {
            authenticateAs(userA1Id, orgA, "a1@orga.com", Set.of("MANAGER", "ROLE_MANAGER"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, userA1Id)).thenReturn(Optional.of(empA1));
            when(employeeRepository.findAllByOrganizationId(orgA)).thenReturn(List.of(empA1, empA2));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

            assertEquals(PracticeSecurityScope.RoleTier.DEPARTMENT_MANAGER, scope.getRoleTier());
            assertTrue(scope.isDepartmentManager());
            assertFalse(scope.isFirmAdmin());

            // Includes colleague A2 in same department ("Direct Tax")
            assertTrue(scope.getAccessibleAssigneeIds().contains(empA1Id));
            assertTrue(scope.getAccessibleAssigneeIds().contains(empA2Id));
        }

        @Test
        @DisplayName("ROLE_ORG_ADMIN / ROLE_PARTNER grants unrestricted firm-wide visibility")
        void testOrgAdminGrantsFirmWideVisibility() {
            authenticateAs(userA1Id, orgA, "a1@orga.com", Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, userA1Id)).thenReturn(Optional.of(empA1));

            PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

            assertEquals(PracticeSecurityScope.RoleTier.FIRM_ADMIN, scope.getRoleTier());
            assertTrue(scope.isFirmAdmin());
            assertNull(scope.getAccessibleAssigneeIds()); // null indicates unrestricted

            Set<UUID> clientIds = securityScopeEvaluator.getAccessibleClientIds(scope);
            assertNull(clientIds); // unrestricted
        }

        @Test
        @DisplayName("Cross-tenant employee assignment is strictly rejected")
        void testCrossTenantEmployeeAssignmentRejected() {
            authenticateAs(userA1Id, orgA, "a1@orga.com", Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, userA1Id)).thenReturn(Optional.of(empA1));
            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));

            // Employee B1 belongs to Org B, not Org A
            when(employeeRepository.findByIdAndOrganizationId(empB1Id, orgA)).thenReturn(Optional.empty());

            AssignClientEmployeeRequest request = new AssignClientEmployeeRequest(empB1Id);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> clientService.assignEmployee(clientA1Id, request));
            assertTrue(ex.getMessage().contains("Employee"));
        }

        @Test
        @DisplayName("Cross-tenant client assignment in updateClient is strictly rejected")
        void testCrossTenantEmployeeAssignmentInUpdateRejected() {
            authenticateAs(userA1Id, orgA, "a1@orga.com", Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"));

            when(employeeRepository.findByOrganizationIdAndUserId(orgA, userA1Id)).thenReturn(Optional.of(empA1));
            when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
            when(employeeRepository.findByIdAndOrganizationId(empB1Id, orgA)).thenReturn(Optional.empty());

            UpdateClientRequest request = UpdateClientRequest.builder()
                    .displayName("Client A1 Updated")
                    .clientType(ClientEntity.ClientType.INDIVIDUAL)
                    .assignedEmployeeId(empB1Id) // Employee from Org B
                    .build();

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> clientService.updateClient(clientA1Id, request));
            assertTrue(ex.getMessage().contains("Assigned Employee"));
        }
    }
}
