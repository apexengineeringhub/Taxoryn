package com.taxoryn.module.employee.chat.service;

import com.taxoryn.module.employee.chat.dto.EmployeeChatChannelDto;
import com.taxoryn.module.employee.chat.dto.EmployeeChatContactDto;
import com.taxoryn.module.employee.chat.dto.EmployeeChatMessageDto;
import com.taxoryn.module.employee.chat.dto.SendEmployeeChatMessageRequest;

import java.util.List;
import java.util.UUID;

public interface EmployeeChatService {

    List<EmployeeChatContactDto> getEligibleContacts();

    List<EmployeeChatChannelDto> getAccessibleChannels();

    List<EmployeeChatMessageDto> getDirectMessages(UUID recipientEmployeeId);

    EmployeeChatMessageDto sendDirectMessage(UUID recipientEmployeeId, SendEmployeeChatMessageRequest request);

    void markDirectMessagesRead(UUID senderEmployeeId);

    List<EmployeeChatMessageDto> getChannelMessages(UUID channelId);

    EmployeeChatMessageDto sendChannelMessage(UUID channelId, SendEmployeeChatMessageRequest request);

    long getGlobalUnreadCount();
}
