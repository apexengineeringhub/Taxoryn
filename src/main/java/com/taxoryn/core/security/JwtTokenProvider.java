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
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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

    @Value("${taxoryn.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${taxoryn.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${taxoryn.jwt.refresh-expiration-ms:604800000}")
    private long jwtRefreshExpirationMs;

    @Value("${taxoryn.jwt.issuer:taxoryn-platform}")
    private String issuer;

    private final TokenBlacklistService tokenBlacklistService;

    public JwtTokenProvider(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService != null ? tokenBlacklistService : new TokenBlacklistService();
    }

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID organizationId, String email, Set<String> roles, Set<String> permissions) {
        return buildToken(userId, organizationId, email, roles, permissions, jwtExpirationMs, "ACCESS");
    }

    public String generateRefreshToken(UUID userId, UUID organizationId, String email) {
        return buildToken(userId, organizationId, email, Set.of(), Set.of(), jwtRefreshExpirationMs, "REFRESH");
    }

    private String buildToken(UUID userId, UUID organizationId, String email, Set<String> roles, Set<String> permissions, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("organizationId", organizationId.toString());
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
