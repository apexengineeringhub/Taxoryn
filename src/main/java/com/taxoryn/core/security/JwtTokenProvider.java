package com.taxoryn.core.security;

import com.taxoryn.core.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * SECURITY: This value is a publicly known placeholder committed to source control
     * (see application.yml / application-prod.yml / application-demo.yml). It exists only
     * as a convenience default for LOCAL DEVELOPMENT. It must never be the effective signing
     * key outside of local/dev/test — see the fail-fast check in init() below. Anyone who can
     * read this repository knows this string, so using it in production allows forging valid
     * JWTs for any user/organization (full authentication bypass).
     */
    private static final String KNOWN_INSECURE_DEFAULT_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private static final Set<String> NON_PRODUCTION_PROFILES = Set.of("local", "dev", "test", "default");

    @Value("${taxoryn.jwt.secret:" + KNOWN_INSECURE_DEFAULT_SECRET + "}")
    private String jwtSecret;

    @Value("${taxoryn.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${taxoryn.jwt.refresh-expiration-ms:604800000}")
    private long jwtRefreshExpirationMs;

    @Value("${taxoryn.jwt.issuer:taxoryn-platform}")
    private String issuer;

    private final TokenBlacklistService tokenBlacklistService;
    private final Environment environment;

    public JwtTokenProvider(TokenBlacklistService tokenBlacklistService, Environment environment) {
        this.tokenBlacklistService = tokenBlacklistService != null ? tokenBlacklistService : new TokenBlacklistService();
        this.environment = environment;
    }

    private SecretKey key;

    @PostConstruct
    public void init() {
        // SECURITY (P0): Refuse to start with the known, publicly-committed default JWT
        // secret in any environment that is not explicitly local/dev/test. This prevents a
        // missing JWT_SECRET environment variable from silently degrading production to a
        // forgeable, publicly-known signing key (full authentication/tenant-isolation bypass).
        boolean isNonProductionProfile = environment == null
                || environment.getActiveProfiles().length == 0
                || Arrays.stream(environment.getActiveProfiles()).anyMatch(NON_PRODUCTION_PROFILES::contains);

        if (KNOWN_INSECURE_DEFAULT_SECRET.equals(jwtSecret) && !isNonProductionProfile) {
            throw new IllegalStateException(
                    "FATAL SECURITY MISCONFIGURATION: taxoryn.jwt.secret / JWT_SECRET is not set (or is set to the " +
                    "publicly-known repository default) while running under a non-local profile (" +
                    (environment != null ? Arrays.toString(environment.getActiveProfiles()) : "unknown") + "). " +
                    "Refusing to start: this default secret is committed to source control and would allow " +
                    "forging authentication tokens for any user or organization. Set a unique JWT_SECRET " +
                    "environment variable (256-bit+ random value) before deploying.");
        }

        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("taxoryn.jwt.secret must be at least 256 bits (32 bytes) for HMAC-SHA256 signing.");
        }

        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID organizationId, String email, Set<String> roles, Set<String> permissions) {
        return generateAccessToken(userId, organizationId, null, email, roles, permissions);
    }

    public String generateAccessToken(UUID userId, UUID organizationId, UUID clientId, String email, Set<String> roles, Set<String> permissions) {
        return buildToken(userId, organizationId, clientId, email, roles, permissions, jwtExpirationMs, "ACCESS");
    }

    public String generateRefreshToken(UUID userId, UUID organizationId, String email) {
        return generateRefreshToken(userId, organizationId, null, email);
    }

    public String generateRefreshToken(UUID userId, UUID organizationId, UUID clientId, String email) {
        return buildToken(userId, organizationId, clientId, email, Set.of(), Set.of(), jwtRefreshExpirationMs, "REFRESH");
    }

    private String buildToken(UUID userId, UUID organizationId, UUID clientId, String email, Set<String> roles, Set<String> permissions, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        if (organizationId != null) {
            claims.put("organizationId", organizationId.toString());
        }
        if (clientId != null) {
            claims.put("clientId", clientId.toString());
        }
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("tokenType", tokenType);

        return Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
            throw new UnauthorizedException("JWT token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid JWT token");
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.get("userId", String.class));
    }

    public UUID getOrganizationIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String orgId = claims.get("organizationId", String.class);
        return orgId != null ? UUID.fromString(orgId) : null;
    }

    public UUID getClientIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String clientId = claims.get("clientId", String.class);
        return clientId != null ? UUID.fromString(clientId) : null;
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toSet());
        }
        return Set.of();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object permsObj = claims.get("permissions");
        if (permsObj instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toSet());
        }
        return Set.of();
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tokenType", String.class);
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("tokenType", String.class);
            return tokenType == null || "ACCESS".equalsIgnoreCase(tokenType);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("tokenType", String.class);
            return "REFRESH".equalsIgnoreCase(tokenType);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(token)) {
            log.debug("Token is revoked / blacklisted");
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public void invalidateToken(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            if (tokenBlacklistService != null) {
                tokenBlacklistService.blacklistToken(token, expiration);
            }
            log.info("Token successfully invalidated");
        } catch (Exception ex) {
            log.warn("Failed to extract expiration for token invalidation: {}", ex.getMessage());
        }
    }
}
