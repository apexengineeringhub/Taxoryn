package com.taxoryn.module.gmail.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gmail.client.GmailApiClient;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.dto.GmailAccountConnectRequest;
import com.taxoryn.module.gmail.dto.GmailAccountDto;
import com.taxoryn.module.gmail.dto.GoogleOAuthTokenResponse;
import com.taxoryn.module.gmail.dto.GoogleUserInfoResponse;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.repository.GmailConversationRepository;
import com.taxoryn.module.gmail.util.TokenEncryptionService;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailOAuthServiceImpl implements GmailOAuthService {

    private final GmailProperties properties;
    private final GmailApiClient apiClient;
    private final TokenEncryptionService encryptionService;
    private final GmailAccountRepository accountRepository;
    private final GmailConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    public String generateAuthorizationUrl(UUID organizationId, UUID userId, String redirectUri) {
        String effectiveRedirectUri = StringUtils.hasText(redirectUri) ? redirectUri : properties.getRedirectUri();
        String state = String.format("%s:%s:%s", organizationId, userId, UUID.randomUUID());

        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&access_type=offline&prompt=consent&state=%s",
                properties.getAuthUrl(),
                URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(effectiveRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(properties.getScope(), StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );
    }

    @Override
    @Transactional
    public GmailAccountDto connectAccount(UUID organizationId, UUID userId, GmailAccountConnectRequest request) {
        log.info("Connecting Gmail account for organizationId={}, userId={}", organizationId, userId);

        GoogleOAuthTokenResponse tokenResponse = apiClient.exchangeAuthCode(request.getAuthCode(), request.getRedirectUri());
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccessToken())) {
            throw new BadRequestException("Google OAuth did not return a valid access token");
        }

        GoogleUserInfoResponse userInfo = apiClient.getUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null || !StringUtils.hasText(userInfo.getEmail())) {
            throw new BadRequestException("Could not retrieve email address from Google account");
        }

        String email = userInfo.getEmail().toLowerCase().trim();
        Instant expiresAt = Instant.now().plus(
                tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 3600,
                ChronoUnit.SECONDS
        );

        Optional<GmailAccountEntity> existingOpt = accountRepository.findByOrganizationIdAndEmailAddress(organizationId, email);

        GmailAccountEntity account;
        if (existingOpt.isPresent()) {
            account = existingOpt.get();
            account.setStatus(GmailAccountStatus.CONNECTED);
            account.setEncryptedAccessToken(encryptionService.encrypt(tokenResponse.getAccessToken()));
            if (StringUtils.hasText(tokenResponse.getRefreshToken())) {
                account.setEncryptedRefreshToken(encryptionService.encrypt(tokenResponse.getRefreshToken()));
            }
            account.setTokenExpiresAt(expiresAt);
            account.setSyncErrorMessage(null);
            if (request.getAccountType() != null) {
                account.setAccountType(request.getAccountType());
            }
            if (request.getAccountType() == GmailAccountType.INDIVIDUAL_PRACTITIONER) {
                account.setUserId(userId);
            }
        } else {
            account = GmailAccountEntity.builder()
                    .emailAddress(email)
                    .accountType(request.getAccountType() != null ? request.getAccountType() : GmailAccountType.PRACTICE_SHARED)
                    .status(GmailAccountStatus.CONNECTED)
                    .userId(request.getAccountType() == GmailAccountType.INDIVIDUAL_PRACTITIONER ? userId : null)
                    .encryptedAccessToken(encryptionService.encrypt(tokenResponse.getAccessToken()))
                    .encryptedRefreshToken(StringUtils.hasText(tokenResponse.getRefreshToken()) ? encryptionService.encrypt(tokenResponse.getRefreshToken()) : null)
                    .tokenExpiresAt(expiresAt)
                    .build();
            account.setOrganizationId(organizationId);
        }

        GmailAccountEntity saved = accountRepository.save(account);

        auditService.logEvent(
                organizationId,
                userId,
                "GMAIL_ACCOUNT_CONNECTED",
                "GMAIL_ACCOUNT",
                saved.getId().toString(),
                null,
                "Connected Gmail mailbox: " + email
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GmailAccountEntity getValidAuthenticatedAccount(UUID organizationId, UUID accountId) {
        GmailAccountEntity account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Gmail account not found with ID: " + accountId));

        if (account.getStatus() != GmailAccountStatus.CONNECTED) {
            throw new BadRequestException("Gmail account is not in CONNECTED status: " + account.getStatus());
        }

        // Check if access token is expired or about to expire in 5 minutes
        Instant now = Instant.now();
        if (account.getTokenExpiresAt() == null || account.getTokenExpiresAt().isBefore(now.plus(5, ChronoUnit.MINUTES))) {
            log.info("Refreshing expired Gmail access token for accountId={}", accountId);
            if (!StringUtils.hasText(account.getEncryptedRefreshToken())) {
                account.setStatus(GmailAccountStatus.AUTH_EXPIRED);
                account.setSyncErrorMessage("Refresh token missing. Re-authentication required.");
                accountRepository.save(account);
                throw new BadRequestException("Gmail authentication expired. Please re-connect the mailbox.");
            }

            try {
                String plainRefreshToken = encryptionService.decrypt(account.getEncryptedRefreshToken());
                GoogleOAuthTokenResponse refreshed = apiClient.refreshAccessToken(plainRefreshToken);

                if (refreshed != null && StringUtils.hasText(refreshed.getAccessToken())) {
                    account.setEncryptedAccessToken(encryptionService.encrypt(refreshed.getAccessToken()));
                    account.setTokenExpiresAt(now.plus(
                            refreshed.getExpiresIn() != null ? refreshed.getExpiresIn() : 3600,
                            ChronoUnit.SECONDS
                    ));
                    account.setSyncErrorMessage(null);
                    account = accountRepository.save(account);
                }
            } catch (Exception ex) {
                log.error("Failed to refresh Gmail token for accountId={}: {}", accountId, ex.getMessage());
                account.setStatus(GmailAccountStatus.AUTH_EXPIRED);
                account.setSyncErrorMessage("Failed to refresh token: " + ex.getMessage());
                accountRepository.save(account);
                throw new BadRequestException("Failed to refresh Gmail token. Please re-authenticate.");
            }
        }

        return account;
    }

    @Override
    @Transactional
    public void disconnectAccount(UUID organizationId, UUID accountId) {
        GmailAccountEntity account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Gmail account not found with ID: " + accountId));

        account.setStatus(GmailAccountStatus.DISCONNECTED);
        account.setEncryptedAccessToken(null);
        account.setEncryptedRefreshToken(null);
        account.setTokenExpiresAt(null);
        accountRepository.save(account);

        auditService.logEvent(
                organizationId,
                null,
                "GMAIL_ACCOUNT_DISCONNECTED",
                "GMAIL_ACCOUNT",
                accountId.toString(),
                "CONNECTED",
                "DISCONNECTED"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GmailAccountDto> listAccounts(UUID organizationId) {
        return accountRepository.findAllByOrganizationId(organizationId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GmailAccountDto getAccount(UUID organizationId, UUID accountId) {
        GmailAccountEntity account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Gmail account not found with ID: " + accountId));
        return mapToDto(account);
    }

    private GmailAccountDto mapToDto(GmailAccountEntity entity) {
        String userName = null;
        if (entity.getUserId() != null) {
            userName = userRepository.findById(entity.getUserId())
                    .map(UserEntity::getFullName)
                    .orElse(null);
        }

        long unread = 0;
        long open = 0;
        long total = 0;
        if (entity.getId() != null) {
            total = conversationRepository.countByOrganizationId(entity.getOrganizationId());
            open = conversationRepository.countByOrganizationIdAndStatus(entity.getOrganizationId(), GmailConversationStatus.OPEN);
            unread = conversationRepository.countByOrganizationIdAndIsUnreadTrue(entity.getOrganizationId());
        }

        return GmailAccountDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .userId(entity.getUserId())
                .userFullName(userName)
                .emailAddress(entity.getEmailAddress())
                .accountType(entity.getAccountType())
                .status(entity.getStatus())
                .lastSyncedAt(entity.getLastSyncedAt())
                .lastHistoryId(entity.getLastHistoryId())
                .syncErrorMessage(entity.getSyncErrorMessage())
                .totalConversations(total)
                .unreadConversations(unread)
                .openConversations(open)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
