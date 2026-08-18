package com.taxoryn.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI taxorynOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Taxoryn API — Practice Management Platform")
                        .description("Modular Monolith REST APIs for Taxoryn Multi-Tenant Practice Management Platform.\n\n" +
                                "### Architecture Highlights\n" +
                                "- **Multi-Tenancy**: Tenant context derived strictly from JWT (`organizationId` claim).\n" +
                                "- **Security**: Stateless Bearer JWT tokens.\n" +
                                "- **Standardized Responses**: Wrapped in `ApiResponse<T>` and `PagedResponse<T>` envelopes.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Taxoryn Engineering Team")
                                .email("dev@taxoryn.com")
                                .url("https://taxoryn.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://taxoryn.com/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token containing `userId`, `organizationId`, and `roles` claims.")));
    }
}
