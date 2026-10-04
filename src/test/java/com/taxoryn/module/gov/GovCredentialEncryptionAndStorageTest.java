package com.taxoryn.module.gov;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovCredentialReferenceDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.entity.GovCredentialReferenceEntity;
import com.taxoryn.module.gov.model.GovCredentialStatus;
import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.repository.GovCredentialReferenceRepository;
import com.taxoryn.module.gov.security.GovSecretEncryptionService;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.spi.GovSecretStorageProvider;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.taxoryn.TaxorynApplication.class)
@ActiveProfiles("test")
public class GovCredentialEncryptionAndStorageTest {

    @Autowired
    private GovSecretEncryptionService encryptionService;

    @Autowired
    private GovSecretStorageProvider secretStorageProvider;

    @Autowired
    private GovernmentConnectionService connectionService;

    @Autowired
    private GovCredentialReferenceRepository credentialRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity testOrg;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Gov Enc Practice " + UUID.randomUUID())
                .email("gov.enc." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        TenantContext.setTenantId(testOrg.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("AES-256-GCM encryption service must round-trip correctly and produce randomized ciphertexts")
    void testEncryptionRoundTripAndRandomIv() {
        String plaintext = "TestGovSecretKey_9876543210#@!";

        String cipher1 = encryptionService.encrypt(plaintext);
        String cipher2 = encryptionService.encrypt(plaintext);

        assertThat(cipher1).isNotBlank();
        assertThat(cipher1).isNotEqualTo(plaintext);
        // AES-GCM uses random 12-byte IV per invocation: two encryptions of same secret must yield different ciphertexts
        assertThat(cipher1).isNotEqualTo(cipher2);

        String decrypted1 = encryptionService.decrypt(cipher1);
        String decrypted2 = encryptionService.decrypt(cipher2);

        assertThat(decrypted1).isEqualTo(plaintext);
        assertThat(decrypted2).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Secret storage SPI must store encrypted ciphertext in database and retrieve plaintext seamlessly")
    void testSecretStorageProviderRoundTrip() {
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.GST)
                .displayName("GST Production Portal")
                .build());

        String secretPlaintext = "ApiKeySecret_GSTN_2026_xyz";

        GovCredentialReferenceDto credDto = connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(conn.getId())
                .credentialType(GovCredentialType.API_KEY)
                .maskedIdentifier("gstn_prod_***")
                .rawSecret(secretPlaintext)
                .build());

        assertThat(credDto).isNotNull();
        assertThat(credDto.getCredentialStatus()).isEqualTo(GovCredentialStatus.VALID);
        assertThat(credDto.getMaskedIdentifier()).isEqualTo("gstn_prod_***");

        // Direct DB inspection: ensure persisted record is encrypted and does NOT contain raw secret
        GovCredentialReferenceEntity persisted = credentialRepository
                .findByIdAndOrganizationId(credDto.getId(), testOrg.getId())
                .orElseThrow();

        assertThat(persisted.getEncryptedSecret()).isNotBlank();
        assertThat(persisted.getEncryptedSecret()).isNotEqualTo(secretPlaintext);
        assertThat(persisted.getEncryptedSecret()).doesNotContain(secretPlaintext);

        // Retrieve secret via service
        String retrievedSecret = connectionService.getDecryptedSecret(conn.getId());
        assertThat(retrievedSecret).isEqualTo(secretPlaintext);
    }

    @Test
    @DisplayName("Invalidating credential must mark status REVOKED and prevent secret retrieval")
    void testCredentialInvalidation() {
        GovConnectionDto conn = connectionService.createConnection(CreateGovConnectionRequest.builder()
                .providerType(GovProviderType.INCOME_TAX)
                .displayName("ITD Portal e-Filing")
                .build());

        GovCredentialReferenceDto credDto = connectionService.registerCredential(RegisterGovCredentialRequest.builder()
                .connectionId(conn.getId())
                .credentialType(GovCredentialType.BASIC_AUTH)
                .maskedIdentifier("itd_user_***")
                .rawSecret("SecretPassword#2026")
                .build());

        // Invalidate credential
        GovCredentialReferenceDto invalidated = connectionService.invalidateCredential(credDto.getId());
        assertThat(invalidated.getCredentialStatus()).isEqualTo(GovCredentialStatus.REVOKED);

        // Attempting to retrieve secret for connection with revoked credential must be denied
        assertThatThrownBy(() -> connectionService.getDecryptedSecret(conn.getId()))
                .hasMessageContaining("credential reference is inactive or expired");
    }
}
