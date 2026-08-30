package com.taxoryn.module.notification.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.notification.dto.NotificationDto;
import com.taxoryn.module.notification.dto.NotificationFilterRequest;
import com.taxoryn.module.notification.dto.SendNotificationRequest;
import com.taxoryn.module.notification.dto.UnreadCountDto;
import com.taxoryn.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/notifications", "/api/notifications"})
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification center: history, unread count, mark-as-read, and multi-channel dispatch")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my notifications", description = "Retrieves the current user's or client's paginated in-app notification history, newest first, with optional read-status and type filters.")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> getNotifications(@Valid @ModelAttribute NotificationFilterRequest filterRequest) {
        PagedResponse<NotificationDto> response = notificationService.getNotifications(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", response));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count", description = "Returns the number of unread in-app notifications for the current user or client, e.g. for a notification bell badge.")
    public ResponseEntity<ApiResponse<UnreadCountDto>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", UnreadCountDto.builder().unreadCount(count).build()));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a notification as read", description = "Marks a single notification owned by the current user or client as read.")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable UUID notificationId) {
        NotificationDto updated = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", updated));
    }

    @PatchMapping("/{notificationId}/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a notification as unread", description = "Marks a single notification owned by the current user or client as unread.")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsUnread(@PathVariable UUID notificationId) {
        NotificationDto updated = notificationService.markAsUnread(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as unread", updated));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read", description = "Marks every unread notification owned by the current user or client as read.")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsReadPatch() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("updated", updated)));
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read (POST)", description = "Marks every unread notification owned by the current user or client as read.")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsReadPost() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("updated", updated)));
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dismiss a notification", description = "Deletes a single notification owned by the current user or client from their history.")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification dismissed", null));
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Send a notification", description = "Manually dispatches a notification to a firm user or client on one or more channels (IN_APP, EMAIL, SMS, WHATSAPP). Intended for administrative/manual use; automated flows call the NotificationService directly.")
    public ResponseEntity<ApiResponse<NotificationDto>> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        NotificationDto sent = notificationService.send(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Notification dispatched successfully", sent));
    }
}
