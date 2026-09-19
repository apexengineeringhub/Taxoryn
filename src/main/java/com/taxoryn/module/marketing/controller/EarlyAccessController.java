package com.taxoryn.module.marketing.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.proxy.ClientIpResolver;
import com.taxoryn.module.marketing.dto.CreateEarlyAccessRequest;
import com.taxoryn.module.marketing.dto.EarlyAccessResponse;
import com.taxoryn.module.marketing.service.EarlyAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/marketing/early-access", "/api/marketing/early-access"})
@RequiredArgsConstructor
@Tag(name = "Marketing Early Access", description = "Public endpoints for Taxoryn practice access requests")
public class EarlyAccessController {

    private final EarlyAccessService earlyAccessService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping
    @Operation(summary = "Submit practice access request", description = "Public unauthenticated endpoint to submit an early access request for a tax practice")
    public ResponseEntity<ApiResponse<EarlyAccessResponse>> submitEarlyAccessRequest(
            @Valid @RequestBody CreateEarlyAccessRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Received practice access request from IP {} for email {}",
                clientIp, EarlyAccessResponse.maskEmail(request.getEmail()));

        EarlyAccessResponse response = earlyAccessService.submitEarlyAccessRequest(request, clientIp, userAgent);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.getMessage(), response));
    }
}
