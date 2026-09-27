package com.taxoryn.module.docrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WorkflowDocumentRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ComplianceWorkflowRepository workflowRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity organization;
    private LocationEntity location;
    private UserEntity adminUser;
    private ClientEntity client;
    private ComplianceObligationEntity obligation;
    private ComplianceWorkflowEntity workflow;
    private String token;

    @BeforeEach
    void setUp() {
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("ORG_ADMIN")
                        .isSystemRole(true)
                        .description("Organization Administrator")
                        .permissions(new HashSet<>())
                        .build()));

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Workflow Audit Hub - " + UUID.randomUUID())
                .legalName("Workflow Audit Hub LLP")
                .email("admin.wf." + UUID.randomUUID() + "@example.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(organization.getId());

        LocationEntity loc = LocationEntity.builder()
                .name("Main Office")
                .code("MO-" + UUID.randomUUID().toString().substring(0, 4))
                .city("New Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(organization.getId());
        location = locationRepository.save(loc);

        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(organization.getId())
                .email("admin.wf." + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Compliance")
                .lastName("Manager")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                organization.getId(),
                adminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_UPDATE", "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_UPLOAD", "COMPLIANCE_VIEW", "COMPLIANCE_WRITE")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Delta Dynamics")
                .legalName("Delta Dynamics Technologies Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .pan("AABCD9999Z")
                .locationId(location.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(organization.getId());
        client = clientRepository.save(c);

        ComplianceObligationEntity obl = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Delta")
                .periodLabel("2026-03")
                .statutoryDueDate(LocalDate.of(2026, 4, 20))
                .status(ComplianceObligationStatus.UPCOMING)
                .locationId(location.getId())
                .build();
        obl.setOrganizationId(organization.getId());
        obligation = obligationRepository.save(obl);

        ComplianceWorkflowEntity wf = ComplianceWorkflowEntity.builder()
                .clientId(client.getId())
                .complianceObligationId(obligation.getId())
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .workflowType("GST_GSTR3B")
                .locationId(location.getId())
                .statutoryDueDate(LocalDate.of(2026, 4, 20))
                .build();
        wf.setOrganizationId(organization.getId());
        workflow = workflowRepository.save(wf);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        documentRequestRepository.deleteAll();
        documentRepository.deleteAll();
        workflowRepository.deleteAll();
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Upload document linked to Compliance Workflow and retrieve via workflow endpoint")
    void testUploadAndRetrieveWorkflowDocuments() throws Exception {
        byte[] fileContent = "%PDF-1.4 GSTR-3B computation sheet".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "GSTR3B_Computation.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .workflowId(workflow.getId())
                .clientId(client.getId())
                .documentType(DocumentType.OTHER)
                .notes("GSTR-3B Tax liability computation working sheet")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.workflowId").value(workflow.getId().toString()))
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()));

        // Retrieve documents by workflow ID
        mockMvc.perform(get("/api/v1/documents/workflows/" + workflow.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fileName").value("GSTR3B_Computation.pdf"))
                .andExpect(jsonPath("$.data[0].workflowId").value(workflow.getId().toString()));
    }

    @Test
    @DisplayName("Create document request linked to Compliance Workflow and retrieve via workflow endpoint")
    void testCreateAndRetrieveWorkflowDocumentRequests() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .workflowId(workflow.getId())
                .clientId(client.getId())
                .purpose("GSTR-3B Sales & Purchase Register Request")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .title("GSTR-1 Outward Sales Register (Excel)")
                                .documentType(DocumentType.GST_INVOICE_SALE)
                                .required(true)
                                .build(),
                        CreateDocumentRequestItem.builder()
                                .title("Inward Purchase Register (Excel)")
                                .documentType(DocumentType.GST_INVOICE_PURCHASE)
                                .required(true)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.workflowId").value(workflow.getId().toString()))
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()));

        // Retrieve requests by workflow ID
        mockMvc.perform(get("/api/v1/document-requests/workflows/" + workflow.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].workflowId").value(workflow.getId().toString()))
                .andExpect(jsonPath("$.data[0].purpose").value("GSTR-3B Sales & Purchase Register Request"))
                .andExpect(jsonPath("$.data[0].items.length()").value(2));
    }
}
