package com.taxoryn.module.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientLocationAssignmentEntity;
import com.taxoryn.module.client.entity.ClientUserAssignmentEntity;
import com.taxoryn.module.client.repository.ClientLocationAssignmentRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientUserAssignmentRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.entity.UserLocationEntity;
import com.taxoryn.module.user.repository.UserLocationRepository;
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
public class DocumentAccessScopeIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private ClientLocationAssignmentRepository clientLocationAssignmentRepository;

    @Autowired
    private ClientUserAssignmentRepository clientUserAssignmentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity organization;
    private LocationEntity headOffice;
    private LocationEntity branchOffice;
    private UserEntity firmAdminUser;
    private UserEntity staffUser1;
    private UserEntity staffUser2;
    private EmployeeEntity employee1;
    private EmployeeEntity employee2;
    private ClientEntity client1;
    private ClientEntity client2;

    private String adminToken;
    private String staff1Token;
    private String staff2Token;

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

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("STAFF")
                        .isSystemRole(true)
                        .description("Staff Member")
                        .permissions(new HashSet<>())
                        .build()));

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Nexus Audit Firm - " + UUID.randomUUID())
                .legalName("Nexus Audit Firm LLP")
                .email("admin." + UUID.randomUUID() + "@nexus.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(organization.getId());

        LocationEntity ho = LocationEntity.builder()
                .name("Mumbai Head Office")
                .code("HO-MUM-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        ho.setOrganizationId(organization.getId());
        headOffice = locationRepository.save(ho);

        LocationEntity bo = LocationEntity.builder()
                .name("Pune Branch")
                .code("BR-PUN-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Pune")
                .state("Maharashtra")
                .isHeadOffice(false)
                .isActive(true)
                .build();
        bo.setOrganizationId(organization.getId());
        branchOffice = locationRepository.save(bo);

        firmAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(organization.getId())
                .email("admin." + UUID.randomUUID() + "@nexus.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("Firm")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                firmAdminUser.getId(),
                organization.getId(),
                firmAdminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_UPDATE", "CLIENT_CREATE", "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_UPLOAD", "DOCUMENT_DELETE")
        );

        staffUser1 = userRepository.save(UserEntity.builder()
                .organizationId(organization.getId())
                .email("staff1." + UUID.randomUUID() + "@nexus.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Vikram")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());

        EmployeeEntity emp1 = EmployeeEntity.builder()
                .userId(staffUser1.getId())
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 6))
                .email(staffUser1.getEmail())
                .firstName(staffUser1.getFirstName())
                .lastName(staffUser1.getLastName())
                .department("Tax")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        emp1.setOrganizationId(organization.getId());
        employee1 = employeeRepository.save(emp1);

        UserLocationEntity ul1 = UserLocationEntity.builder()
                .id(UserLocationEntity.UserLocationId.builder()
                        .userId(staffUser1.getId())
                        .locationId(headOffice.getId())
                        .build())
                .organizationId(organization.getId())
                .build();
        userLocationRepository.save(ul1);

        staff1Token = jwtTokenProvider.generateAccessToken(
                staffUser1.getId(),
                organization.getId(),
                staffUser1.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "CLIENT_VIEW", "CLIENT_UPDATE", "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_UPLOAD")
        );

        staffUser2 = userRepository.save(UserEntity.builder()
                .organizationId(organization.getId())
                .email("staff2." + UUID.randomUUID() + "@nexus.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Pooja")
                .lastName("Deshmukh")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(staffRole))
                .build());

        EmployeeEntity emp2 = EmployeeEntity.builder()
                .userId(staffUser2.getId())
                .employeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 6))
                .email(staffUser2.getEmail())
                .firstName(staffUser2.getFirstName())
                .lastName(staffUser2.getLastName())
                .department("Audit")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        emp2.setOrganizationId(organization.getId());
        employee2 = employeeRepository.save(emp2);

        UserLocationEntity ul2 = UserLocationEntity.builder()
                .id(UserLocationEntity.UserLocationId.builder()
                        .userId(staffUser2.getId())
                        .locationId(branchOffice.getId())
                        .build())
                .organizationId(organization.getId())
                .build();
        userLocationRepository.save(ul2);

        staff2Token = jwtTokenProvider.generateAccessToken(
                staffUser2.getId(),
                organization.getId(),
                staffUser2.getEmail(),
                Set.of("ROLE_STAFF"),
                Set.of("ROLE_STAFF", "CLIENT_VIEW", "CLIENT_UPDATE", "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_UPLOAD")
        );

        ClientEntity c1 = ClientEntity.builder()
                .displayName("Client One Mumbai")
                .legalName("Client One Mumbai Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .pan("ABCDE1111A")
                .locationId(headOffice.getId())
                .assignedEmployeeId(employee1.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c1.setOrganizationId(organization.getId());
        client1 = clientRepository.save(c1);

        ClientLocationAssignmentEntity cla1 = ClientLocationAssignmentEntity.builder()
                .clientId(client1.getId())
                .locationId(headOffice.getId())
                .primaryLocation(true)
                .active(true)
                .build();
        cla1.setOrganizationId(organization.getId());
        clientLocationAssignmentRepository.save(cla1);

        ClientUserAssignmentEntity cua1 = ClientUserAssignmentEntity.builder()
                .clientId(client1.getId())
                .userId(staffUser1.getId())
                .active(true)
                .primaryResponsible(true)
                .build();
        cua1.setOrganizationId(organization.getId());
        clientUserAssignmentRepository.save(cua1);

        ClientEntity c2 = ClientEntity.builder()
                .displayName("Client Two Pune")
                .legalName("Client Two Pune LLP")
                .clientType(ClientType.PARTNERSHIP)
                .pan("XYZPQ2222B")
                .locationId(branchOffice.getId())
                .assignedEmployeeId(employee2.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c2.setOrganizationId(organization.getId());
        client2 = clientRepository.save(c2);

        ClientLocationAssignmentEntity cla2 = ClientLocationAssignmentEntity.builder()
                .clientId(client2.getId())
                .locationId(branchOffice.getId())
                .primaryLocation(true)
                .active(true)
                .build();
        cla2.setOrganizationId(organization.getId());
        clientLocationAssignmentRepository.save(cla2);

        ClientUserAssignmentEntity cua2 = ClientUserAssignmentEntity.builder()
                .clientId(client2.getId())
                .userId(staffUser2.getId())
                .active(true)
                .primaryResponsible(true)
                .build();
        cua2.setOrganizationId(organization.getId());
        clientUserAssignmentRepository.save(cua2);

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
        clientUserAssignmentRepository.deleteAll();
        clientLocationAssignmentRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userLocationRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Staff can upload and view documents for in-scope client")
    void testStaffInScopeClientAccess() throws Exception {
        byte[] fileContent = "%PDF-1.4 sample file for client 1".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "C1_Doc.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .locationId(headOffice.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientId").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.locationId").value(headOffice.getId().toString()));
    }

    @Test
    @DisplayName("Staff is forbidden from uploading document for out-of-scope client")
    void testStaffOutOfScopeClientUploadForbidden() throws Exception {
        byte[] fileContent = "%PDF-1.4 unauthorized upload attempt".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "C2_Doc.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .clientId(client2.getId())
                .locationId(branchOffice.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        // Staff 1 attempting to upload to Client 2 -> 403 Forbidden
        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Staff is forbidden from viewing out-of-scope client document vault")
    void testStaffOutOfScopeVaultForbidden() throws Exception {
        // Staff 1 trying to view Client 2's vault -> 403 Forbidden
        mockMvc.perform(get("/api/v1/documents/clients/" + client2.getId())
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Document listing respects staff location and client portfolio scope")
    void testDocumentListingScopeEnforcement() throws Exception {
        // Admin uploads document for Client 1
        MockMultipartFile f1 = new MockMultipartFile("file", "Doc1.pdf", "application/pdf", "%PDF-1.4 Client1".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile m1 = new MockMultipartFile("metadata", "", "application/json",
                objectMapper.writeValueAsBytes(UploadDocumentRequest.builder().clientId(client1.getId()).locationId(headOffice.getId()).documentType(DocumentType.PAN_CARD).build()));
        mockMvc.perform(multipart("/api/v1/documents/upload").file(f1).file(m1).header("Authorization", "Bearer " + adminToken)).andExpect(status().isCreated());

        // Admin uploads document for Client 2
        MockMultipartFile f2 = new MockMultipartFile("file", "Doc2.pdf", "application/pdf", "%PDF-1.4 Client2".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile m2 = new MockMultipartFile("metadata", "", "application/json",
                objectMapper.writeValueAsBytes(UploadDocumentRequest.builder().clientId(client2.getId()).locationId(branchOffice.getId()).documentType(DocumentType.PAN_CARD).build()));
        mockMvc.perform(multipart("/api/v1/documents/upload").file(f2).file(m2).header("Authorization", "Bearer " + adminToken)).andExpect(status().isCreated());

        // Staff 1 lists documents -> only sees Client 1 document
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].clientId").value(client1.getId().toString()));

        // Admin lists documents -> sees both documents
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("Document requests listing respects staff portfolio scope")
    void testDocumentRequestListingScopeEnforcement() throws Exception {
        // Create request for Client 1
        CreateDocumentRequest req1 = CreateDocumentRequest.builder()
                .clientId(client1.getId())
                .locationId(headOffice.getId())
                .purpose("Client 1 Request")
                .items(List.of(CreateDocumentRequestItem.builder().title("Doc A").documentType(DocumentType.OTHER).build()))
                .build();
        mockMvc.perform(post("/api/v1/document-requests").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        // Create request for Client 2
        CreateDocumentRequest req2 = CreateDocumentRequest.builder()
                .clientId(client2.getId())
                .locationId(branchOffice.getId())
                .purpose("Client 2 Request")
                .items(List.of(CreateDocumentRequestItem.builder().title("Doc B").documentType(DocumentType.OTHER).build()))
                .build();
        mockMvc.perform(post("/api/v1/document-requests").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        // Staff 1 lists requests -> only sees Client 1 request
        mockMvc.perform(get("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].clientId").value(client1.getId().toString()));

        // Admin lists requests -> sees both requests
        mockMvc.perform(get("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }
}
