package com.taxoryn.core.security;

import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Security utility for managing HttpOnly, Secure, SameSite refresh token cookies.
 * Shields authentication refresh credentials from JavaScript (XSS mitigation).
 */
@Component
public class AuthCookieUtil {

    private final String cookieName;
    private final boolean cookieSecure;
    private final String sameSitePolicy;
    private final String cookiePath;
    private final String cookieDomain;
    private final long refreshExpirationMs;

    public AuthCookieUtil(
            @Value("${taxoryn.auth.cookie.name:taxoryn_refresh_token}") String cookieName,
            @Value("${taxoryn.auth.cookie.secure:${TAXORYN_COOKIE_SECURE:false}}") boolean cookieSecure,
            @Value("${taxoryn.auth.cookie.same-site:${TAXORYN_COOKIE_SAME_SITE:Lax}}") String sameSitePolicy,
            @Value("${taxoryn.auth.cookie.path:${TAXORYN_COOKIE_PATH:/}}") String cookiePath,
            @Value("${taxoryn.auth.cookie.domain:${TAXORYN_COOKIE_DOMAIN:}}") String cookieDomain,
            @Value("${taxoryn.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.sameSitePolicy = sameSitePolicy;
        this.cookiePath = cookiePath;
        this.cookieDomain = cookieDomain;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Creates an HttpOnly, Secure (in prod), SameSite cookie holding the raw refresh token.
     */
    public ResponseCookie createRefreshTokenCookie(String rawRefreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .sameSite(sameSitePolicy);

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain.trim());
        }

        return builder.build();
    }

    /**
     * Creates a deletion cookie with Max-Age=0 matching the exact cookie path/domain.
     */
    public ResponseCookie createDeleteRefreshTokenCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(0)
                .sameSite(sameSitePolicy);

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain.trim());
        }

        return builder.build();
    }

    /**
     * Resolves the refresh token:
     * 1. Checks HttpOnly cookie (browser clients).
     * 2. Falls back to request body if present (headless/API clients).
     */
    public Optional<String> extractRefreshToken(HttpServletRequest request, RefreshTokenRequest body) {
        // 1. Check cookies first
        if (request.getCookies() != null) {
            Optional<String> cookieToken = Arrays.stream(request.getCookies())
                    .filter(c -> cookieName.equals(c.getName()) || "refresh_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(StringUtils::hasText)
                    .findFirst();

            if (cookieToken.isPresent()) {
                return cookieToken;
            }
        }

        // 2. Fall back to body (for mobile / automated integration test clients)
        if (body != null && StringUtils.hasText(body.getRefreshToken())) {
            return Optional.of(body.getRefreshToken().trim());
        }

        return Optional.empty();
    }

    public String getCookieName() {
        return cookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public String getSameSitePolicy() {
        return sameSitePolicy;
    }

    public String getCookiePath() {
        return cookiePath;
    }
}
