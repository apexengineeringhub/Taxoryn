package com.taxoryn.module.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client ITR Filing Status")
public class ClientItrStatusDto {

    private UUID id;
    private String assessmentYear;
    private String financialYear;
    private String itrType;
    private String taxpayerType;
    private LocalDate dueDate;
    private LocalDate filingDate;
    private String acknowledgementNumber;
    private String status;
}
