package com.taxoryn.module.client.businesscontext.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextDto;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;
import com.taxoryn.module.client.businesscontext.service.BusinessContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-context")
@RequiredArgsConstructor
@Tag(name = "Business Context Resolver", description = "Cross-domain contextual read-model resolution endpoints")
public class BusinessContextController {

    private final BusinessContextResolver businessContextResolver;

    @PostMapping("/resolve")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_STAFF') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('ASSOCIATE') or hasRole('ORG_ADMIN')")
    @Operation(summary = "Resolve composite business context across Client, Service, Engagement, Work, and Attention domains")
    public ResponseEntity<ApiResponse<BusinessContextDto>> resolveContext(@Valid @RequestBody BusinessContextRequest request) {
        BusinessContextDto context = businessContextResolver.resolveContext(request);
        return ResponseEntity.ok(ApiResponse.success(context));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_STAFF') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('ASSOCIATE') or hasRole('ORG_ADMIN')")
    @Operation(summary = "Resolve client-centric business context")
    public ResponseEntity<ApiResponse<BusinessContextDto>> resolveClientContext(@PathVariable UUID clientId) {
        BusinessContextDto context = businessContextResolver.resolveClientContext(clientId);
        return ResponseEntity.ok(ApiResponse.success(context));
    }
}
