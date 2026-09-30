package com.taxoryn.module.worktemplate.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderTasksRequest {

    @NotEmpty(message = "Ordered task IDs list cannot be empty")
    private List<UUID> orderedTaskIds;
}
