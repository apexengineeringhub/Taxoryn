package com.taxoryn.module.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAssigneeDto {
    private UUID userId;
    private String name;
    private String email;
    private String role;
}
