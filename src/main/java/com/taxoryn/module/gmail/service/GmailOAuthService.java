package com.taxoryn.module.gmail.service;

import com.taxoryn.module.gmail.dto.GmailAccountConnectRequest;
import com.taxoryn.module.gmail.dto.GmailAccountDto;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;

import java.util.List;
import java.util.UUID;

public interface GmailOAuthService {

    String generateAuthorizationUrl(UUID organizationId, UUID userId, String redirectUri);

    GmailAccountDto connectAccount(UUID organizationId, UUID userId, GmailAccountConnectRequest request);

    GmailAccountEntity getValidAuthenticatedAccount(UUID organizationId, UUID accountId);

    void disconnectAccount(UUID organizationId, UUID accountId);

    List<GmailAccountDto> listAccounts(UUID organizationId);

    GmailAccountDto getAccount(UUID organizationId, UUID accountId);
}
