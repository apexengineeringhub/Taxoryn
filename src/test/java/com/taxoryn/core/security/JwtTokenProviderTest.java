package com.taxoryn.core.security;

import com.taxoryn.core.exception.UnauthorizedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService tokenBlacklistService;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        MockEnvironment testEnvironment = new MockEnvironment();
        testEnvironment.setActiveProfiles("test");
        tokenBlacklistService = new TokenBlacklistService();
        jwtTokenProvider = new JwtTokenProvider(tokenBlacklistService, testEnvironment);
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
        UUID clientId = UUID.randomUUID();
        String email = "ca.partner@taxpractice.com";
        Set<String> roles = Set.of("ORG_ADMIN", "CA_PARTNER");
        Set<String> permissions = Set.of("GST_READ", "GST_WRITE", "ITR_READ");

        String token = jwtTokenProvider.generateAccessToken(userId, organizationId, clientId, email, roles, permissions);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.isAccessToken(token));
        assertFalse(jwtTokenProvider.isRefreshToken(token));

        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(organizationId, jwtTokenProvider.getOrganizationIdFromToken(token));
        assertEquals(clientId, jwtTokenProvider.getClientIdFromToken(token));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
        assertEquals("ACCESS", jwtTokenProvider.getTokenTypeFromToken(token));

        Set<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);
        assertTrue(extractedRoles.contains("ORG_ADMIN"));
        assertTrue(extractedRoles.contains("CA_PARTNER"));

        Set<String> extractedPermissions = jwtTokenProvider.getPermissionsFromToken(token);
        assertTrue(extractedPermissions.contains("GST_READ"));
        assertTrue(extractedPermissions.contains("GST_WRITE"));
    }

    @Test
    @DisplayName("Generate Refresh Token and verify tokenType separation")
    void testGenerateRefreshTokenAndVerifyTokenType() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String email = "user@taxpractice.com";

        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, organizationId, email);

        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertFalse(jwtTokenProvider.isAccessToken(refreshToken));
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        assertEquals("REFRESH", jwtTokenProvider.getTokenTypeFromToken(refreshToken));
    }

    @Test
    @DisplayName("SECURITY: Expired token is rejected")
    void testExpiredTokenValidationFails() {
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        Date past = new Date(System.currentTimeMillis() - 100000);
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(past)
                .issuedAt(new Date(past.getTime() - 100000))
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
        assertThrows(UnauthorizedException.class, () -> jwtTokenProvider.getClaimsFromToken(expiredToken));
    }

    @Test
    @DisplayName("SECURITY: Tampered signature / wrong key is rejected")
    void testTamperedSignatureRejected() {
        String wrongSecret = "999E635266556A586E3272357538782F413F4428472B4B6250645367566B5999";
        SecretKey attackerKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", UUID.randomUUID().toString());
        claims.put("roles", Set.of("PLATFORM_SUPER_ADMIN"));

        String forgedToken = Jwts.builder()
                .subject("attacker")
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(attackerKey)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(forgedToken));
        assertThrows(UnauthorizedException.class, () -> jwtTokenProvider.getClaimsFromToken(forgedToken));
    }

    @Test
    @DisplayName("SECURITY: Tampered payload bytes is rejected")
    void testTamperedPayloadRejected() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, organizationId, "user@test.com", Set.of("STAFF"), Set.of());

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String tamperedPayload = parts[1] + "tamper";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("SECURITY: Blacklisted / Revoked token is rejected")
    void testRevokedTokenRejected() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, organizationId, "user@test.com", Set.of("ORG_ADMIN"), Set.of());

        assertTrue(jwtTokenProvider.validateToken(token));

        jwtTokenProvider.invalidateToken(token);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Validate invalid or corrupted token returns false")
    void testValidateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    @DisplayName("SECURITY: refuses to start with known default secret under a production-like profile")
    void testFailsFastOnDefaultSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");

        JwtTokenProvider provider = new JwtTokenProvider(new TokenBlacklistService(), prodEnvironment);
        ReflectionTestUtils.setField(provider, "jwtSecret", testSecret);

        assertThrows(IllegalStateException.class, provider::init,
                "JwtTokenProvider must refuse to start when the publicly-known default secret " +
                "would be used to sign tokens under a non-local/dev/test profile.");
    }

    @Test
    @DisplayName("SECURITY: refuses to start with short secret (< 32 bytes)")
    void testFailsFastOnShortSecret() {
        MockEnvironment devEnvironment = new MockEnvironment();
        devEnvironment.setActiveProfiles("dev");

        JwtTokenProvider provider = new JwtTokenProvider(new TokenBlacklistService(), devEnvironment);
        ReflectionTestUtils.setField(provider, "jwtSecret", "too-short-secret");

        assertThrows(IllegalStateException.class, provider::init);
    }

    @Test
    @DisplayName("SECURITY: starts normally with a unique secret under a production-like profile")
    void testStartsWithUniqueSecretInProduction() {
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");

        JwtTokenProvider provider = new JwtTokenProvider(new TokenBlacklistService(), prodEnvironment);
        ReflectionTestUtils.setField(provider, "jwtSecret", "a-unique-randomly-generated-production-secret-value-1234567890");

        provider.init();
    }
}
