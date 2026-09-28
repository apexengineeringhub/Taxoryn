package com.taxoryn.module.gmail.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.gmail.dto.GmailAccountConnectRequest;
import com.taxoryn.module.gmail.dto.GmailAccountDto;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import com.taxoryn.module.gmail.service.GmailOAuthService;
import com.taxoryn.module.gmail.service.GmailSyncService;
import com.taxoryn.module.gmail.service.GmailSyncService.GmailSyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/gmail/accounts")
@RequiredArgsConstructor
@Tag(name = "Gmail Mailbox Management", description = "Endpoints for connecting, configuring, syncing, and disconnecting practice Gmail accounts")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("isAuthenticated()")
public class GmailAccountController {

    private final GmailOAuthService oAuthService;
    private final GmailSyncService syncService;
    private final PracticeSecurityScopeEvaluator scopeEvaluator;

    @GetMapping("/authorize-url")
    @Operation(summary = "Generate Google OAuth authorization URL for mailbox connection")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthorizeUrl(
            @RequestParam(value = "redirectUri", required = false) String redirectUri
    ) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        String url = oAuthService.generateAuthorizationUrl(scope.getOrganizationId(), scope.getUserId(), redirectUri);
        return ResponseEntity.ok(ApiResponse.success(Map.of("authorizationUrl", url)));
    }

    @PostMapping("/connect")
    @Operation(summary = "Exchange Google OAuth authorization code to connect a Gmail mailbox")
    public ResponseEntity<ApiResponse<GmailAccountDto>> connectAccount(
            @Valid @RequestBody GmailAccountConnectRequest request
    ) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailAccountDto account = oAuthService.connectAccount(scope.getOrganizationId(), scope.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Gmail account connected successfully", account));
    }

    @GetMapping
    @Operation(summary = "List all connected Gmail mailboxes for the practice")
    public ResponseEntity<ApiResponse<List<GmailAccountDto>>> listAccounts() {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        List<GmailAccountDto> accounts = oAuthService.listAccounts(scope.getOrganizationId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get connected Gmail mailbox profile by ID")
    public ResponseEntity<ApiResponse<GmailAccountDto>> getAccount(@PathVariable UUID accountId) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailAccountDto account = oAuthService.getAccount(scope.getOrganizationId(), accountId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/{accountId}/sync")
    @Operation(summary = "Trigger on-demand metadata synchronization for a connected Gmail account")
    public ResponseEntity<ApiResponse<GmailSyncResult>> triggerSync(
            @PathVariable UUID accountId,
            @RequestParam(value = "syncType", defaultValue = "INCREMENTAL") GmailSyncType syncType
    ) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        GmailSyncResult result = syncService.syncAccount(scope.getOrganizationId(), accountId, syncType);
        return ResponseEntity.ok(ApiResponse.success("Sync completed with status: " + result.getStatus(), result));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Disconnect and unlink a Gmail mailbox from the practice")
    public ResponseEntity<ApiResponse<Void>> disconnectAccount(@PathVariable UUID accountId) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        oAuthService.disconnectAccount(scope.getOrganizationId(), accountId);
        return ResponseEntity.ok(ApiResponse.success("Gmail account disconnected successfully", null));
    }
}
