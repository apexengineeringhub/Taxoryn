package com.taxoryn.module.gmail.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.module.gmail.dto.GmailConversationDto;
import com.taxoryn.module.gmail.dto.GmailConversationFilterRequest;
import com.taxoryn.module.gmail.dto.GmailConversationUpdateDto;

import java.util.UUID;

public interface GmailConversationService {

    PagedResponse<GmailConversationDto> getConversations(GmailConversationFilterRequest filterRequest, PracticeSecurityScope scope);

    GmailConversationDto getConversation(UUID conversationId, PracticeSecurityScope scope);

    GmailConversationDto updateConversation(UUID conversationId, GmailConversationUpdateDto request, PracticeSecurityScope scope);

    GmailConversationDto assignConversation(UUID conversationId, UUID assignedUserId, PracticeSecurityScope scope);

    GmailConversationDto linkClient(UUID conversationId, UUID clientId, PracticeSecurityScope scope);

    GmailConversationDto unlinkClient(UUID conversationId, PracticeSecurityScope scope);
}
