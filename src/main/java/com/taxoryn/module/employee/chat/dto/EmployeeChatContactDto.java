package com.taxoryn.module.employee.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeChatContactDto {
    private UUID employeeId;
    private UUID userId;
    private String employeeCode;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private String designation;
    private String department;
    private String role;
    private String status;
    private String lastMessagePreview;
    private Instant lastMessageTimestamp;
    private Long unreadCount;
    private Boolean isManager;
    private Boolean isSelf;
}
