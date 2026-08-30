package com.taxoryn.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        MockEnvironment testEnvironment = new MockEnvironment();
        testEnvironment.setActiveProfiles("test");
        jwtTokenProvider = new JwtTokenProvider(new TokenBlacklistService(), testEnvironment);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshExpirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "issuer", "taxoryn-test");
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Generate Access Token and verify multi-tenant claims")
    void testGenerateAccessTokenAndVerifyClaims() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String email = "ca.partner@taxpractice.com";
        Set<String> roles = Set.of("ORG_ADMIN", "CA_PARTNER");
        Set<String> permissions = Set.of("GST_READ", "GST_WRITE", "ITR_READ");

        String token = jwtTokenProvider.generateAccessToken(userId, organizationId, email, roles, permissions);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(organizationId, jwtTokenProvider.getOrganizationIdFromToken(token));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(token));

        Set<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);
        assertTrue(extractedRoles.contains("ORG_ADMIN"));
        assertTrue(extractedRoles.contains("CA_PARTNER"));

        Set<String> extractedPermissions = jwtTokenProvider.getPermissionsFromToken(token);
        assertTrue(extractedPermissions.contains("GST_READ"));
        assertTrue(extractedPermissions.contains("GST_WRITE"));
    }

    @Test
    @DisplayName("Validate invalid or corrupted token returns false")
    void testValidateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
    }

    @Test
    @DisplayName("SECURITY: refuses to start with known default secret under a production-like profile")
    void testFailsFastOnDefaultSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");

        JwtTokenProvider provider = new JwtTokenProvider(new TokenBlacklistService(), prodEnvironment);
        ReflectionTestUtils.setField(provider, "jwtSecret", testSecret); // the known committed default

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, provider::init,
                "JwtTokenProvider must refuse to start when the publicly-known default secret " +
                "would be used to sign tokens under a non-local/dev/test profile.");
    }

    @Test
    @DisplayName("SECURITY: starts normally with a unique secret under a production-like profile")
    void testStartsWithUniqueSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");

        JwtTokenProvider provider = new JwtTokenProvider(new TokenBlacklistService(), prodEnvironment);
        ReflectionTestUtils.setField(provider, "jwtSecret", "a-unique-randomly-generated-production-secret-value-1234567890");

        provider.init(); // should not throw
    }
}
