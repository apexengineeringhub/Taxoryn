package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.CreateClientBranchRequest;
import com.taxoryn.module.client.dto.CreateClientContactRequest;
import com.taxoryn.module.client.dto.CreateClientRelationshipRequest;
import com.taxoryn.module.client.dto.UpdateClientBranchRequest;
import com.taxoryn.module.client.dto.UpdateClientContactRequest;
import com.taxoryn.module.client.entity.ClientBranchEntity;
import com.taxoryn.module.client.entity.ClientBranchType;
import com.taxoryn.module.client.entity.ClientContactEntity;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientRelationshipEntity;
import com.taxoryn.module.client.entity.ClientRelationshipType;
import com.taxoryn.module.client.entity.ContactRole;
import com.taxoryn.module.client.repository.ClientBranchRepository;
import com.taxoryn.module.client.repository.ClientContactRepository;
import com.taxoryn.module.client.repository.ClientRelationshipRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientContactBranchAndRelationshipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientContactRepository clientContactRepository;

    @Autowired
    private ClientBranchRepository clientBranchRepository;

    @Autowired
    private ClientRelationshipRepository clientRelationshipRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private String authToken1;
    private ClientEntity client1;
    private ClientEntity client2;
    private ClientEntity client3;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        clientRelationshipRepository.deleteAll();
        clientContactRepository.deleteAll();
        clientBranchRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA & Advisors LLP")
                .legalName("Apex Chartered Accountants LLP")
                .email("admin@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Create Organization 2
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Tax Firm")
                .legalName("Other Tax Firm Pvt Ltd")
                .email("admin@othertax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Admin User in Org 1
        TenantContext.setTenantId(org1.getId());

        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());

        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@apexca.com")
                .firstName("Rajesh")
                .lastName("Sharma")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build());

        authToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_READ", "CLIENT_WRITE", "CLIENT_CREATE", "CLIENT_UPDATE", "CLIENT_DELETE")
        );

        // 5. Create sample clients in Org 1
        ClientEntity c1 = ClientEntity.builder()
                .displayName("Reliance Industries Ltd")
                .legalName("Reliance Industries Limited")
                .pan("AAACR1234K")
                .gstin("27AAACR1234K1ZV")
                .clientType(ClientType.PUBLIC_LIMITED)
                .status(ClientStatus.ACTIVE)
                .dateOfIncorporation(LocalDate.of(1973, 5, 8))
                .addressLine1("Maker Chambers IV, Nariman Point")
                .city("Mumbai")
                .state("Maharashtra")
                .stateCode("27")
                .pincode("400021")
                .build();
        c1.setOrganizationId(org1.getId());
        client1 = clientRepository.save(c1);

        ClientEntity c2 = ClientEntity.builder()
                .displayName("Jio Platforms Ltd")
                .legalName("Jio Platforms Limited")
                .pan("AAACJ5678L")
                .clientType(ClientType.PUBLIC_LIMITED)
                .status(ClientStatus.ACTIVE)
                .city("Navi Mumbai")
                .state("Maharashtra")
                .stateCode("27")
                .build();
        c2.setOrganizationId(org1.getId());
        client2 = clientRepository.save(c2);

        // 6. Create client in Org 2
        TenantContext.setTenantId(org2.getId());
        organizationModuleRepository.save(OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build());

        ClientEntity c3 = ClientEntity.builder()
                .displayName("Tata Motors Ltd")
                .legalName("Tata Motors Limited")
                .pan("AAACT9999M")
                .clientType(ClientType.PUBLIC_LIMITED)
                .status(ClientStatus.ACTIVE)
                .city("Mumbai")
                .state("Maharashtra")
                .build();
        c3.setOrganizationId(org2.getId());
        client3 = clientRepository.save(c3);

        TenantContext.setTenantId(org1.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==========================================
    // PART A: CLIENT CONTACTS TESTS
    // ==========================================

    @Test
    @DisplayName("Create client contact successfully")
    void testCreateContact_Success() throws Exception {
        CreateClientContactRequest request = CreateClientContactRequest.builder()
                .firstName("Mukesh")
                .lastName("Ambani")
                .designation("Managing Director")
                .email("mukesh.ambani@ril.com")
                .phone("+91 9820012345")
                .contactRole(ContactRole.DIRECTOR)
                .primaryContact(true)
                .notes("Key executive contact")
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/contacts")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Mukesh"))
                .andExpect(jsonPath("$.data.lastName").value("Ambani"))
                .andExpect(jsonPath("$.data.displayName").value("Mukesh Ambani"))
                .andExpect(jsonPath("$.data.designation").value("Managing Director"))
                .andExpect(jsonPath("$.data.contactRole").value("DIRECTOR"))
                .andExpect(jsonPath("$.data.primaryContact").value(true))
                .andExpect(jsonPath("$.data.active").value(true));

        List<ClientContactEntity> contacts = clientContactRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryContactDescCreatedAtAsc(org1.getId(), client1.getId());
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).isPrimaryContact()).isTrue();
    }

    @Test
    @DisplayName("Creating a second primary contact atomically unsets the previous primary contact")
    void testCreateContact_PrimaryContact_UnsetsPreviousPrimary() throws Exception {
        // Create first contact as primary
        CreateClientContactRequest request1 = CreateClientContactRequest.builder()
                .firstName("Alok")
                .lastName("Agarwal")
                .designation("CFO")
                .email("alok.agarwal@ril.com")
                .contactRole(ContactRole.FINANCE)
                .primaryContact(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/contacts")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Create second contact also as primary
        CreateClientContactRequest request2 = CreateClientContactRequest.builder()
                .firstName("Srikanth")
                .lastName("Venkatachari")
                .designation("Joint CFO")
                .email("srikanth.v@ril.com")
                .contactRole(ContactRole.PRIMARY)
                .primaryContact(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/contacts")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.firstName").value("Srikanth"))
                .andExpect(jsonPath("$.data.primaryContact").value(true));

        // Verify only Srikanth is primary, Alok is no longer primary
        List<ClientContactEntity> contacts = clientContactRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryContactDescCreatedAtAsc(org1.getId(), client1.getId());
        assertThat(contacts).hasSize(2);

        ClientContactEntity primary = contacts.stream().filter(ClientContactEntity::isPrimaryContact).findFirst().orElseThrow();
        assertThat(primary.getFirstName()).isEqualTo("Srikanth");

        long primaryCount = contacts.stream().filter(ClientContactEntity::isPrimaryContact).count();
        assertThat(primaryCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Set existing contact to primary successfully switches primary status")
    void testSetPrimaryContact_Success() throws Exception {
        ClientContactEntity c1 = ClientContactEntity.builder()
                .clientId(client1.getId())
                .firstName("First")
                .primaryContact(true)
                .active(true)
                .build();
        c1.setOrganizationId(org1.getId());
        c1 = clientContactRepository.save(c1);

        ClientContactEntity c2 = ClientContactEntity.builder()
                .clientId(client1.getId())
                .firstName("Second")
                .primaryContact(false)
                .active(true)
                .build();
        c2.setOrganizationId(org1.getId());
        c2 = clientContactRepository.save(c2);

        mockMvc.perform(put("/api/v1/clients/" + client1.getId() + "/contacts/" + c2.getId() + "/primary")
                        .header("Authorization", authToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryContact").value(true));

        ClientContactEntity updatedC1 = clientContactRepository.findById(c1.getId()).orElseThrow();
        ClientContactEntity updatedC2 = clientContactRepository.findById(c2.getId()).orElseThrow();

        assertThat(updatedC1.isPrimaryContact()).isFalse();
        assertThat(updatedC2.isPrimaryContact()).isTrue();
    }

    @Test
    @DisplayName("Setting an inactive contact as primary is rejected")
    void testSetPrimaryContact_InactiveContact_Rejects() throws Exception {
        ClientContactEntity c = ClientContactEntity.builder()
                .clientId(client1.getId())
                .firstName("Inactive")
                .primaryContact(false)
                .active(false)
                .build();
        c.setOrganizationId(org1.getId());
        c = clientContactRepository.save(c);

        mockMvc.perform(put("/api/v1/clients/" + client1.getId() + "/contacts/" + c.getId() + "/primary")
                        .header("Authorization", authToken1))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update client contact and deactivation")
    void testUpdateAndDeactivateContact() throws Exception {
        ClientContactEntity contact = ClientContactEntity.builder()
                .clientId(client1.getId())
                .firstName("Pankaj")
                .lastName("Pawar")
                .designation("VP")
                .primaryContact(true)
                .active(true)
                .build();
        contact.setOrganizationId(org1.getId());
        contact = clientContactRepository.save(contact);

        UpdateClientContactRequest updateReq = UpdateClientContactRequest.builder()
                .designation("Senior VP - Operations")
                .phone("+91 9999988888")
                .build();

        mockMvc.perform(put("/api/v1/clients/" + client1.getId() + "/contacts/" + contact.getId())
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.designation").value("Senior VP - Operations"))
                .andExpect(jsonPath("$.data.phone").value("+91 9999988888"));

        // Deactivate contact
        mockMvc.perform(delete("/api/v1/clients/" + client1.getId() + "/contacts/" + contact.getId())
                        .header("Authorization", authToken1))
                .andExpect(status().isOk());

        ClientContactEntity deactivated = clientContactRepository.findById(contact.getId()).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();
        assertThat(deactivated.isPrimaryContact()).isFalse();
    }

    @Test
    @DisplayName("Client contact tenant isolation prevents cross-tenant access")
    void testContact_TenantIsolation() throws Exception {
        TenantContext.setTenantId(org2.getId());
        ClientContactEntity contact;
        try {
            contact = ClientContactEntity.builder()
                    .clientId(client3.getId())
                    .firstName("Ratan")
                    .lastName("Tata")
                    .active(true)
                    .build();
            contact.setOrganizationId(org2.getId());
            contact = clientContactRepository.save(contact);
        } finally {
            TenantContext.clear();
        }

        // Org 1 tries to access Org 2's contact
        mockMvc.perform(get("/api/v1/clients/" + client3.getId() + "/contacts/" + contact.getId())
                        .header("Authorization", authToken1))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // PART B: CLIENT BRANCHES TESTS
    // ==========================================

    @Test
    @DisplayName("Create client branch successfully")
    void testCreateBranch_Success() throws Exception {
        CreateClientBranchRequest request = CreateClientBranchRequest.builder()
                .branchName("Jamnagar Manufacturing Complex")
                .branchCode("JAM-01")
                .branchType(ClientBranchType.FACTORY)
                .addressLine1("Village Motikhavdi, Digvijaygram")
                .city("Jamnagar")
                .state("Gujarat")
                .stateCode("24")
                .pincode("361140")
                .gstin("24AAACR1234K1Z4")
                .primaryBranch(false)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/branches")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchName").value("Jamnagar Manufacturing Complex"))
                .andExpect(jsonPath("$.data.branchCode").value("JAM-01"))
                .andExpect(jsonPath("$.data.branchType").value("FACTORY"))
                .andExpect(jsonPath("$.data.stateCode").value("24"))
                .andExpect(jsonPath("$.data.gstin").value("24AAACR1234K1Z4"))
                .andExpect(jsonPath("$.data.active").value(true));

        List<ClientBranchEntity> branches = clientBranchRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryBranchDescCreatedAtAsc(org1.getId(), client1.getId());
        assertThat(branches).hasSize(1);
    }

    @Test
    @DisplayName("Creating a primary branch unsets previous primary branch")
    void testCreateBranch_PrimaryBranch_UnsetsPreviousPrimary() throws Exception {
        CreateClientBranchRequest b1 = CreateClientBranchRequest.builder()
                .branchName("Nariman Point Office")
                .branchType(ClientBranchType.REGISTERED_OFFICE)
                .city("Mumbai")
                .primaryBranch(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/branches")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b1)))
                .andExpect(status().isCreated());

        CreateClientBranchRequest b2 = CreateClientBranchRequest.builder()
                .branchName("Navi Mumbai HQ")
                .branchType(ClientBranchType.PRINCIPAL_PLACE_OF_BUSINESS)
                .city("Navi Mumbai")
                .primaryBranch(true)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/branches")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primaryBranch").value(true));

        List<ClientBranchEntity> branches = clientBranchRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryBranchDescCreatedAtAsc(org1.getId(), client1.getId());
        assertThat(branches).hasSize(2);

        ClientBranchEntity primary = branches.stream().filter(ClientBranchEntity::isPrimaryBranch).findFirst().orElseThrow();
        assertThat(primary.getBranchName()).isEqualTo("Navi Mumbai HQ");

        long primaryCount = branches.stream().filter(ClientBranchEntity::isPrimaryBranch).count();
        assertThat(primaryCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Set existing branch to primary and deactivate branch")
    void testSetPrimaryBranchAndDeactivate() throws Exception {
        ClientBranchEntity branch1 = ClientBranchEntity.builder()
                .clientId(client1.getId())
                .branchName("Branch 1")
                .primaryBranch(true)
                .active(true)
                .build();
        branch1.setOrganizationId(org1.getId());
        branch1 = clientBranchRepository.save(branch1);

        ClientBranchEntity branch2 = ClientBranchEntity.builder()
                .clientId(client1.getId())
                .branchName("Branch 2")
                .primaryBranch(false)
                .active(true)
                .build();
        branch2.setOrganizationId(org1.getId());
        branch2 = clientBranchRepository.save(branch2);

        mockMvc.perform(put("/api/v1/clients/" + client1.getId() + "/branches/" + branch2.getId() + "/primary")
                        .header("Authorization", authToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryBranch").value(true));

        ClientBranchEntity updatedB1 = clientBranchRepository.findById(branch1.getId()).orElseThrow();
        ClientBranchEntity updatedB2 = clientBranchRepository.findById(branch2.getId()).orElseThrow();
        assertThat(updatedB1.isPrimaryBranch()).isFalse();
        assertThat(updatedB2.isPrimaryBranch()).isTrue();

        // Deactivate branch2
        mockMvc.perform(delete("/api/v1/clients/" + client1.getId() + "/branches/" + branch2.getId())
                        .header("Authorization", authToken1))
                .andExpect(status().isOk());

        ClientBranchEntity deactivated = clientBranchRepository.findById(branch2.getId()).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();
        assertThat(deactivated.isPrimaryBranch()).isFalse();
    }

    @Test
    @DisplayName("Client branch tenant isolation")
    void testBranch_TenantIsolation() throws Exception {
        TenantContext.setTenantId(org2.getId());
        ClientBranchEntity branch;
        try {
            branch = ClientBranchEntity.builder()
                    .clientId(client3.getId())
                    .branchName("Pune Plant")
                    .active(true)
                    .build();
            branch.setOrganizationId(org2.getId());
            branch = clientBranchRepository.save(branch);
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/v1/clients/" + client3.getId() + "/branches/" + branch.getId())
                        .header("Authorization", authToken1))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // PART C: CLIENT RELATIONSHIPS & GROUPS TESTS
    // ==========================================

    @Test
    @DisplayName("Create client relationship successfully")
    void testCreateRelationship_Success() throws Exception {
        CreateClientRelationshipRequest request = CreateClientRelationshipRequest.builder()
                .targetClientId(client2.getId())
                .relationshipType(ClientRelationshipType.SUBSIDIARY)
                .notes("Strategic digital services subsidiary")
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/relationships")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceClientId").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.sourceClientDisplayName").value("Reliance Industries Ltd"))
                .andExpect(jsonPath("$.data.targetClientId").value(client2.getId().toString()))
                .andExpect(jsonPath("$.data.targetClientDisplayName").value("Jio Platforms Ltd"))
                .andExpect(jsonPath("$.data.relationshipType").value("SUBSIDIARY"));

        List<ClientRelationshipEntity> rels = clientRelationshipRepository.findAllForClient(org1.getId(), client1.getId());
        assertThat(rels).hasSize(1);
    }

    @Test
    @DisplayName("Creating a relationship with self is strictly rejected")
    void testCreateRelationship_SelfRelationship_Rejects() throws Exception {
        CreateClientRelationshipRequest request = CreateClientRelationshipRequest.builder()
                .targetClientId(client1.getId())
                .relationshipType(ClientRelationshipType.RELATED_ENTITY)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/relationships")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Creating duplicate active relationship of same type is rejected")
    void testCreateRelationship_Duplicate_Rejects() throws Exception {
        CreateClientRelationshipRequest request = CreateClientRelationshipRequest.builder()
                .targetClientId(client2.getId())
                .relationshipType(ClientRelationshipType.SUBSIDIARY)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/relationships")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt duplicate
        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/relationships")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Creating cross-tenant relationship is rejected")
    void testCreateRelationship_CrossTenant_Rejects() throws Exception {
        // client3 belongs to org2, client1 belongs to org1
        CreateClientRelationshipRequest request = CreateClientRelationshipRequest.builder()
                .targetClientId(client3.getId())
                .relationshipType(ClientRelationshipType.HOLDING_COMPANY)
                .build();

        mockMvc.perform(post("/api/v1/clients/" + client1.getId() + "/relationships")
                        .header("Authorization", authToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete client relationship successfully")
    void testDeleteRelationship_Success() throws Exception {
        ClientRelationshipEntity rel = ClientRelationshipEntity.builder()
                .sourceClientId(client1.getId())
                .targetClientId(client2.getId())
                .relationshipType(ClientRelationshipType.SUBSIDIARY)
                .active(true)
                .build();
        rel.setOrganizationId(org1.getId());
        rel = clientRelationshipRepository.save(rel);

        mockMvc.perform(delete("/api/v1/clients/" + client1.getId() + "/relationships/" + rel.getId())
                        .header("Authorization", authToken1))
                .andExpect(status().isOk());

        assertThat(clientRelationshipRepository.findById(rel.getId())).isEmpty();
    }

    // ==========================================
    // PART D: CLIENT 360 & CONTEXT INTEGRATION
    // ==========================================

    @Test
    @DisplayName("Client 360 and Client Context endpoints reflect contact, branch, and relationship summaries")
    void testClient360AndContext_Summaries() throws Exception {
        ClientContactEntity contact;
        ClientBranchEntity branch;
        ClientRelationshipEntity rel;
        TenantContext.setTenantId(org1.getId());
        try {
            // 1. Add primary contact
            contact = ClientContactEntity.builder()
                    .clientId(client1.getId())
                    .firstName("Nita")
                    .lastName("Ambani")
                    .primaryContact(true)
                    .active(true)
                    .build();
            contact.setOrganizationId(org1.getId());
            contact = clientContactRepository.save(contact);

            // 2. Add primary branch
            branch = ClientBranchEntity.builder()
                    .clientId(client1.getId())
                    .branchName("Mumbai Corporate HQ")
                    .primaryBranch(true)
                    .active(true)
                    .build();
            branch.setOrganizationId(org1.getId());
            branch = clientBranchRepository.save(branch);

            // 3. Add relationship
            rel = ClientRelationshipEntity.builder()
                    .sourceClientId(client1.getId())
                    .targetClientId(client2.getId())
                    .relationshipType(ClientRelationshipType.SUBSIDIARY)
                    .active(true)
                    .build();
            rel.setOrganizationId(org1.getId());
            rel = clientRelationshipRepository.save(rel);
        } finally {
            TenantContext.clear();
        }

        // Verify Client 360
        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/360")
                        .header("Authorization", authToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryContact.displayName").value("Nita Ambani"))
                .andExpect(jsonPath("$.data.contactsCount").value(1))
                .andExpect(jsonPath("$.data.activeContactsCount").value(1))
                .andExpect(jsonPath("$.data.primaryBranch.branchName").value("Mumbai Corporate HQ"))
                .andExpect(jsonPath("$.data.branchesCount").value(1))
                .andExpect(jsonPath("$.data.activeBranchesCount").value(1))
                .andExpect(jsonPath("$.data.relationshipsCount").value(1));

        // Verify Client Context
        mockMvc.perform(get("/api/v1/clients/" + client1.getId() + "/context")
                        .header("Authorization", authToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryContactId").value(contact.getId().toString()))
                .andExpect(jsonPath("$.data.primaryBranchId").value(branch.getId().toString()))
                .andExpect(jsonPath("$.data.contactsCount").value(1))
                .andExpect(jsonPath("$.data.branchesCount").value(1))
                .andExpect(jsonPath("$.data.relationshipsCount").value(1));
    }
}
