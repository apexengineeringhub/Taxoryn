package com.taxoryn.module.employee.chat.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.employee.chat.dto.EmployeeChatChannelDto;
import com.taxoryn.module.employee.chat.dto.EmployeeChatContactDto;
import com.taxoryn.module.employee.chat.dto.EmployeeChatMessageDto;
import com.taxoryn.module.employee.chat.dto.SendEmployeeChatMessageRequest;
import com.taxoryn.module.employee.chat.service.EmployeeChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/employee/chat")
@RequiredArgsConstructor
@Tag(name = "Organization Employee Chat", description = "Endpoints for internal team messaging and department collaboration channels")
@PreAuthorize("isAuthenticated()")
public class EmployeeChatController {

    private final EmployeeChatService employeeChatService;

    @GetMapping("/contacts")
    @Operation(summary = "Get eligible chat contacts", description = "Retrieves contacts list filtered by organization visibility and departmental policy.")
    public ResponseEntity<ApiResponse<List<EmployeeChatContactDto>>> getEligibleContacts() {
        List<EmployeeChatContactDto> contacts = employeeChatService.getEligibleContacts();
        return ResponseEntity.ok(ApiResponse.success("Contacts retrieved successfully", contacts));
    }

    @GetMapping("/channels")
    @Operation(summary = "Get accessible team channels", description = "Retrieves team channels available to caller based on department and role.")
    public ResponseEntity<ApiResponse<List<EmployeeChatChannelDto>>> getAccessibleChannels() {
        List<EmployeeChatChannelDto> channels = employeeChatService.getAccessibleChannels();
        return ResponseEntity.ok(ApiResponse.success("Channels retrieved successfully", channels));
    }

    @GetMapping("/direct/{employeeId}/messages")
    @Operation(summary = "Get 1-on-1 direct messages", description = "Retrieves chronological direct message history with a scoped colleague.")
    public ResponseEntity<ApiResponse<List<EmployeeChatMessageDto>>> getDirectMessages(@PathVariable UUID employeeId) {
        List<EmployeeChatMessageDto> messages = employeeChatService.getDirectMessages(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Direct messages retrieved successfully", messages));
    }

    @PostMapping("/direct/{employeeId}/messages")
    @Operation(summary = "Send 1-on-1 direct message", description = "Sends a direct message to a colleague and broadcasts in real-time via WebSocket.")
    public ResponseEntity<ApiResponse<EmployeeChatMessageDto>> sendDirectMessage(
            @PathVariable UUID employeeId,
            @Valid @RequestBody SendEmployeeChatMessageRequest request) {
        EmployeeChatMessageDto message = employeeChatService.sendDirectMessage(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent successfully", message));
    }

    @PostMapping("/direct/{employeeId}/read")
    @Operation(summary = "Mark direct messages read", description = "Updates read receipts for messages sent by the specified colleague.")
    public ResponseEntity<ApiResponse<Void>> markDirectMessagesRead(@PathVariable UUID employeeId) {
        employeeChatService.markDirectMessagesRead(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    @GetMapping("/channels/{channelId}/messages")
    @Operation(summary = "Get channel messages", description = "Retrieves chronological message feed for an accessible channel.")
    public ResponseEntity<ApiResponse<List<EmployeeChatMessageDto>>> getChannelMessages(@PathVariable UUID channelId) {
        List<EmployeeChatMessageDto> messages = employeeChatService.getChannelMessages(channelId);
        return ResponseEntity.ok(ApiResponse.success("Channel messages retrieved successfully", messages));
    }

    @PostMapping("/channels/{channelId}/messages")
    @Operation(summary = "Send channel message", description = "Posts a message to a team channel and broadcasts to active channel subscribers.")
    public ResponseEntity<ApiResponse<EmployeeChatMessageDto>> sendChannelMessage(
            @PathVariable UUID channelId,
            @Valid @RequestBody SendEmployeeChatMessageRequest request) {
        EmployeeChatMessageDto message = employeeChatService.sendChannelMessage(channelId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Channel message posted successfully", message));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get global unread message count", description = "Returns the total number of unread direct messages for the current employee.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = employeeChatService.getGlobalUnreadCount();
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", Map.of("unreadCount", count)));
    }
}
