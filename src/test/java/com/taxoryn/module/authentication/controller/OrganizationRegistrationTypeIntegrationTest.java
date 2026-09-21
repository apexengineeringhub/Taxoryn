package com.taxoryn.module.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.authentication.dto.RegisterOrganizationRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationRegistrationTypeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest(name = "Registration succeeds for OrganizationType: {0}")
    @EnumSource(value = OrganizationType.class, names = {"SOLO_PRACTITIONER", "SMALL_TAX_FIRM", "GROWING_PRACTICE", "BUSINESS"})
    @DisplayName("Verify successful registration with all valid supported OrganizationType values")
    void testRegisterOrganization_WithValidOrganizationTypes(OrganizationType orgType) throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String orgEmail = "org." + unique + "@taxoryntest.com";
        String adminEmail = "admin." + unique + "@taxoryntest.com";

        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Practice " + orgType.name() + " " + unique)
                .organizationEmail(orgEmail)
                .organizationPhone("+919876543210")
                .pan("AAAPA1234A")
                .gstin("27AAAPA1234A1Z5")
                .organizationType(orgType)
                .adminFirstName("Rajesh")
                .adminLastName("Verma")
                .adminEmail(adminEmail)
                .adminPassword("SecureP@ssword123!")
                .adminPhone("+919876543211")
                .build();

        mockMvc.perform(post("/api/v1/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")))
                .andExpect(jsonPath("$.data.organizationName", is(request.getOrganizationName())))
                .andExpect(jsonPath("$.data.adminEmail", is(adminEmail)));

        OrganizationEntity savedOrg = organizationRepository.findByEmailIgnoreCase(orgEmail)
                .orElseThrow(() -> new AssertionError("Organization should be persisted"));

        assertEquals(OrganizationStatus.INACTIVE, savedOrg.getStatus());
        assertEquals(orgType, savedOrg.getOrganizationType(), "Persisted organizationType must match requested enum");

        UserEntity savedUser = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new AssertionError("Admin user should be persisted"));
        assertEquals(UserStatus.INACTIVE, savedUser.getStatus());
        assertEquals(savedOrg.getId(), savedUser.getOrganizationId());
    }

    @Test
    @DisplayName("Verify registration rejection when organizationType is null or omitted")
    void testRegisterOrganization_RejectsNullOrganizationType() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Null Type Org " + unique)
                .organizationEmail("nulltype." + unique + "@taxoryntest.com")
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail("nulladmin." + unique + "@taxoryntest.com")
                .adminPassword("SecureP@ssword123!")
                .organizationType(null)
                .build();

        mockMvc.perform(post("/api/v1/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Verify registration rejection when organizationType is UNKNOWN")
    void testRegisterOrganization_RejectsUnknownOrganizationType() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Unknown Type Org " + unique)
                .organizationEmail("unknowntype." + unique + "@taxoryntest.com")
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail("unknownadmin." + unique + "@taxoryntest.com")
                .adminPassword("SecureP@ssword123!")
                .organizationType(OrganizationType.UNKNOWN)
                .build();

        mockMvc.perform(post("/api/v1/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Verify registration rejection when organizationType has invalid enum value")
    void testRegisterOrganization_RejectsInvalidEnumString() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        String invalidJson = "{"
                + "\"organizationName\": \"Invalid Type Org " + unique + "\","
                + "\"organizationEmail\": \"invalid." + unique + "@taxoryntest.com\","
                + "\"adminFirstName\": \"Rajesh\","
                + "\"adminLastName\": \"Sharma\","
                + "\"adminEmail\": \"invalidadmin." + unique + "@taxoryntest.com\","
                + "\"adminPassword\": \"SecureP@ssword123!\","
                + "\"organizationType\": \"ENTERPRISE\""
                + "}";

        mockMvc.perform(post("/api/v1/auth/register-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
