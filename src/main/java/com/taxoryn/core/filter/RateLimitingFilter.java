package com.taxoryn.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.response.ErrorResponse;
import com.taxoryn.core.security.RateLimitingService;
import com.taxoryn.core.security.RateLimitingService.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip static resources, Swagger, Actuator
        if (isExcludedPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        boolean isAuth = isAuthEndpoint(uri);

        RateLimitResult result = rateLimitingService.checkRateLimit(clientIp, isAuth);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));

        if (!result.isAllowed()) {
            response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
            log.warn("Rate limit exceeded for IP {} on URI {} (limit: {}, retry-after: {}s)",
                    clientIp, uri, result.getLimit(), result.getRetryAfterSeconds());

            sendRateLimitResponse(request, response, result.getRetryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.startsWith("/api/auth/") || uri.startsWith("/api/v1/auth/");
    }

    private boolean isExcludedPath(String uri) {
        return uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/api-docs") ||
               uri.startsWith("/actuator") ||
               uri.startsWith("/webjars");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xfHeader)) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "0.0.0.0";
    }

    private void sendRateLimitResponse(HttpServletRequest request, HttpServletResponse response, long retryAfter)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .errorCode("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. You have exceeded your API request quota. Please retry in " + retryAfter + " seconds.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .traceId(MDC.get("traceId"))
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
