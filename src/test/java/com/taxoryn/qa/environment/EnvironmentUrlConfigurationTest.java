package com.taxoryn.qa.environment;

import com.taxoryn.module.notification.email.config.EmailProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentUrlConfigurationTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    private EmailProperties bindEmailProperties(String profileYaml) throws IOException {
        MutablePropertySources propertySources = new MutablePropertySources();

        // 1. Load base application.yml
        List<PropertySource<?>> baseSources = loader.load("application.yml", new ClassPathResource("application.yml"));
        for (PropertySource<?> source : baseSources) {
            propertySources.addLast(source);
        }

        // 2. Load profile-specific yaml if provided
        if (profileYaml != null) {
            List<PropertySource<?>> profileSources = loader.load(profileYaml, new ClassPathResource(profileYaml));
            for (PropertySource<?> source : profileSources) {
                propertySources.addFirst(source);
            }
        }

        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
        org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver placeholdersResolver =
                new org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver(propertySources);
        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources), placeholdersResolver, null, null, null);

        EmailProperties properties = binder.bind("taxoryn.mail", Bindable.of(EmailProperties.class))
                .orElseGet(EmailProperties::new);

        // Resolve placeholders in URLs if not automatically resolved by simple binder
        if (properties.getFrontendUrl() != null && properties.getFrontendUrl().contains("${")) {
            properties.setFrontendUrl(resolver.resolvePlaceholders(properties.getFrontendUrl()));
        }
        if (properties.getActivationUrl() != null && properties.getActivationUrl().contains("${")) {
            properties.setActivationUrl(resolver.resolvePlaceholders(properties.getActivationUrl()));
        }
        if (properties.getLoginUrl() != null && properties.getLoginUrl().contains("${")) {
            properties.setLoginUrl(resolver.resolvePlaceholders(properties.getLoginUrl()));
        }

        return properties;
    }

    @Test
    @DisplayName("Local Profile: Activation URL points to localhost:5173 and not production app.taxoryn.com")
    void testLocalProfileUrls() throws Exception {
        EmailProperties properties = bindEmailProperties("application-local.yml");

        assertEquals("http://localhost:5173", properties.getFrontendUrl());
        assertEquals("http://localhost:5173/activate", properties.getActivationUrl());
        assertEquals("http://localhost:5173/login", properties.getLoginUrl());
        assertEquals("info@taxoryn.com", properties.getFromEmail());
        assertEquals("Taxoryn", properties.getFromName());

        // Verify full activation link structure
        String token = "local-test-token-12345";
        String fullActivationUrl = properties.getActivationUrl() + "?token=" + token;
        assertEquals("http://localhost:5173/activate?token=local-test-token-12345", fullActivationUrl);
        assertFalse(fullActivationUrl.contains("app.taxoryn.com"));
        assertFalse(fullActivationUrl.contains("vercel.app"));
    }

    @Test
    @DisplayName("Dev Profile: Activation URL points to localhost:5173")
    void testDevProfileUrls() throws Exception {
        EmailProperties properties = bindEmailProperties("application-dev.yml");

        assertEquals("http://localhost:5173", properties.getFrontendUrl());
        assertEquals("http://localhost:5173/activate", properties.getActivationUrl());
        assertEquals("http://localhost:5173/login", properties.getLoginUrl());

        String token = "dev-test-token-12345";
        String fullActivationUrl = properties.getActivationUrl() + "?token=" + token;
        assertEquals("http://localhost:5173/activate?token=dev-test-token-12345", fullActivationUrl);
        assertFalse(fullActivationUrl.contains("app.taxoryn.com"));
        assertFalse(fullActivationUrl.contains("vercel.app"));
    }

    @Test
    @DisplayName("Demo Profile: Activation URL points to configured demo Vercel domain")
    void testDemoProfileUrls() throws Exception {
        EmailProperties properties = bindEmailProperties("application-demo.yml");

        assertEquals("https://taxoryn-7x7f.vercel.app", properties.getFrontendUrl());
        assertEquals("https://taxoryn-7x7f.vercel.app/activate", properties.getActivationUrl());
        assertEquals("https://taxoryn-7x7f.vercel.app/login", properties.getLoginUrl());
        assertEquals("info@taxoryn.com", properties.getFromEmail());

        String token = "demo-test-token-12345";
        String fullActivationUrl = properties.getActivationUrl() + "?token=" + token;
        assertEquals("https://taxoryn-7x7f.vercel.app/activate?token=demo-test-token-12345", fullActivationUrl);
        assertFalse(fullActivationUrl.contains("localhost"));
        assertFalse(fullActivationUrl.contains("app.taxoryn.com"));
    }

    @Test
    @DisplayName("Production Profile: Activation URL points to app.taxoryn.com and sender is info@taxoryn.com")
    void testProductionProfileUrls() throws Exception {
        EmailProperties properties = bindEmailProperties("application-prod.yml");

        assertEquals("https://app.taxoryn.com", properties.getFrontendUrl());
        assertEquals("https://app.taxoryn.com/activate", properties.getActivationUrl());
        assertEquals("https://app.taxoryn.com/login", properties.getLoginUrl());
        assertEquals("info@taxoryn.com", properties.getFromEmail());
        assertEquals("Taxoryn", properties.getFromName());

        String token = "prod-test-token-12345";
        String fullActivationUrl = properties.getActivationUrl() + "?token=" + token;
        assertEquals("https://app.taxoryn.com/activate?token=prod-test-token-12345", fullActivationUrl);
        assertFalse(fullActivationUrl.contains("localhost"));
        assertFalse(fullActivationUrl.contains("vercel.app"));
        assertFalse(fullActivationUrl.contains("taxoryn-7x7f"));
    }

    @Test
    @DisplayName("Production Profile: Password reset and login URLs resolve to app.taxoryn.com")
    void testProductionPasswordResetAndLoginUrls() throws Exception {
        EmailProperties properties = bindEmailProperties("application-prod.yml");

        String resetToken = "reset-tok-998877";
        String fullResetUrl = properties.getFrontendUrl() + "/reset-password?token=" + resetToken;
        assertEquals("https://app.taxoryn.com/reset-password?token=reset-tok-998877", fullResetUrl);
        assertEquals("https://app.taxoryn.com/login", properties.getLoginUrl());
    }

    @Test
    @DisplayName("Production Profile: CORS origins in application-prod.yml allow app.taxoryn.com and do NOT contain *.vercel.app")
    void testProductionCorsOrigins() throws Exception {
        MutablePropertySources propertySources = new MutablePropertySources();
        List<PropertySource<?>> baseSources = loader.load("application.yml", new ClassPathResource("application.yml"));
        for (PropertySource<?> source : baseSources) {
            propertySources.addLast(source);
        }
        List<PropertySource<?>> prodSources = loader.load("application-prod.yml", new ClassPathResource("application-prod.yml"));
        for (PropertySource<?> source : prodSources) {
            propertySources.addFirst(source);
        }

        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
        String corsOrigins = resolver.getProperty("taxoryn.cors.allowed-origins");

        assertNotNull(corsOrigins);
        assertTrue(corsOrigins.contains("https://app.taxoryn.com"));
        assertTrue(corsOrigins.contains("https://taxoryn.com"));
        assertFalse(corsOrigins.contains("*.vercel.app"));
        assertFalse(corsOrigins.contains("taxoryn-7x7f.vercel.app"));
        assertFalse(corsOrigins.contains("localhost"));
    }

    @Test
    @DisplayName("Default base application.yml fallback URLs point safely to localhost:5173")
    void testBaseApplicationYamlUrls() throws Exception {
        EmailProperties properties = bindEmailProperties(null);

        assertEquals("http://localhost:5173", properties.getFrontendUrl());
        assertEquals("http://localhost:5173/activate", properties.getActivationUrl());
        assertEquals("http://localhost:5173/login", properties.getLoginUrl());
    }
}
