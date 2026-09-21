package com.taxoryn.module.organization.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.organization.dto.CreateOrganizationRequest;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.mapper.OrganizationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationTypeTest {

    private final OrganizationMapper organizationMapper = Mappers.getMapper(OrganizationMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Verify OrganizationType enum contains exactly the 5 required supported values")
    void testOrganizationTypeEnumValues() {
        Set<String> enumNames = Arrays.stream(OrganizationType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(5, enumNames.size(), "OrganizationType must have exactly 5 supported values");
        assertTrue(enumNames.contains("UNKNOWN"));
        assertTrue(enumNames.contains("SOLO_PRACTITIONER"));
        assertTrue(enumNames.contains("SMALL_TAX_FIRM"));
        assertTrue(enumNames.contains("GROWING_PRACTICE"));
        assertTrue(enumNames.contains("BUSINESS"));

        // Verify prohibited legacy / competing names are NOT present
        assertFalse(enumNames.contains("SOLO"));
        assertFalse(enumNames.contains("FIRM"));
        assertFalse(enumNames.contains("ENTERPRISE"));
    }

    @Test
    @DisplayName("Verify OrganizationEntity defaults organizationType to UNKNOWN")
    void testOrganizationEntityDefaultOrganizationType() {
        OrganizationEntity entity = OrganizationEntity.builder()
                .name("Apex Advisors")
                .email("contact@apex.com")
                .build();

        assertNotNull(entity.getOrganizationType());
        assertEquals(OrganizationType.UNKNOWN, entity.getOrganizationType());
    }

    @Test
    @DisplayName("Verify CreateOrganizationRequest defaults organizationType to UNKNOWN")
    void testCreateOrganizationRequestDefault() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Apex Advisors")
                .email("contact@apex.com")
                .build();

        assertNotNull(request.getOrganizationType());
        assertEquals(OrganizationType.UNKNOWN, request.getOrganizationType());
    }

    @Test
    @DisplayName("Verify MapStruct maps all OrganizationType values between Entity and DTO")
    void testMapperMappingAllValues() {
        for (OrganizationType type : OrganizationType.values()) {
            OrganizationEntity entity = OrganizationEntity.builder()
                    .name("Test " + type.name())
                    .email("test" + type.name().toLowerCase() + "@taxoryn.com")
                    .organizationType(type)
                    .build();
            entity.setId(UUID.randomUUID());

            OrganizationDto dto = organizationMapper.toDto(entity);

            assertNotNull(dto);
            assertEquals(type, dto.getOrganizationType(), "DTO organizationType must match entity for " + type);
        }
    }

    @Test
    @DisplayName("Verify JSON Serialization and Deserialization of OrganizationDto with OrganizationType")
    void testJsonSerialization() throws Exception {
        OrganizationDto dto = OrganizationDto.builder()
                .id(UUID.randomUUID())
                .name("Solo Practice CA")
                .email("solo@taxoryn.com")
                .organizationType(OrganizationType.SOLO_PRACTITIONER)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"organizationType\":\"SOLO_PRACTITIONER\""));

        OrganizationDto deserialized = objectMapper.readValue(json, OrganizationDto.class);
        assertEquals(OrganizationType.SOLO_PRACTITIONER, deserialized.getOrganizationType());
    }
}
