package com.taxoryn.core.config;

import com.taxoryn.core.security.CustomUserDetailsService;
import com.taxoryn.core.security.JwtAccessDeniedHandler;
import com.taxoryn.core.security.JwtAuthenticationEntryPoint;
import com.taxoryn.core.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final com.taxoryn.core.filter.RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomUserDetailsService userDetailsService;

    @Value("${taxoryn.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173,http://localhost:8080,https://app.taxoryn.com,https://taxoryn.com}}")
    private String allowedOrigins;

    @Value("${taxoryn.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD}")
    private String allowedMethods;

    @Value("${taxoryn.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${taxoryn.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${taxoryn.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Auth & Onboarding endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/auth/**",
                                "/api/subscriptions/plans",
                                "/api/v1/subscriptions/plans",
                                "/api/marketplace/search",
                                "/api/marketplace/featured",
                                "/api/marketplace/profiles/**",
                                "/api/marketplace/leads",
                                "/api/marketplace/consultations",
                                "/api/marketplace/reviews/**",
                                "/api/marketplace/tax-services/**",
                                "/api/v1/marketplace/search",
                                "/api/v1/marketplace/featured",
                                "/api/v1/marketplace/profiles/**",
                                "/api/v1/marketplace/leads",
                                "/api/v1/marketplace/consultations",
                                "/api/v1/marketplace/reviews/**",
                                "/api/v1/marketplace/tax-services/**",
                                "/api/v1/marketplace/onboarding/proposal/**",
                                "/api/v1/marketplace/onboarding/session/**",
                                "/api/marketplace/customer/register",
                                "/api/v1/marketplace/customer/register",
                                "/api/public/content/**",
                                "/api/v1/public/content/**",
                                "/api/public/media/**",
                                "/api/v1/public/media/**",
                                "/api/notifications/whatsapp/webhook",
                                "/api/v1/notifications/whatsapp/webhook",
                                "/api/v1/public/seo/**",
                                "/robots.txt",
                                "/sitemap.xml"
                        ).permitAll()
                        // Swagger & OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Actuator Health & Metrics
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // Lightweight liveness endpoint for Render keep-alive / external uptime monitors.
                        // No auth, no DB access - see com.taxoryn.core.health.HealthController.
                        .requestMatchers("/", "/api/health", "/favicon.ico", "/error").permitAll()
                        // All other API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(perms -> perms.policy("camera=(), microphone=(), geolocation=()"))
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(org.springframework.util.StringUtils::hasText)
                .toList();

        List<String> methods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .filter(org.springframework.util.StringUtils::hasText)
                .toList();

        List<String> headers = "*".equals(allowedHeaders.trim())
                ? List.of("*")
                : Arrays.stream(allowedHeaders.split(","))
                        .map(String::trim)
                        .filter(org.springframework.util.StringUtils::hasText)
                        .toList();

        // Security Guard: Wildcard origin '*' cannot be combined with allowCredentials=true
        if (allowCredentials && origins.contains("*")) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: CORS wildcard origin '*' cannot be combined with allow-credentials=true");
        }

        // Use setAllowedOriginPatterns only if wildcard pattern is used, otherwise set exact allowed origins
        boolean hasPattern = origins.stream().anyMatch(o -> o.contains("*"));
        if (hasPattern) {
            configuration.setAllowedOriginPatterns(origins);
        } else {
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(methods.isEmpty() ? List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD") : methods);
        configuration.setAllowedHeaders(headers.isEmpty() ? List.of("*") : headers);
        configuration.setExposedHeaders(List.of("X-Trace-Id", "Authorization", "Set-Cookie"));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
