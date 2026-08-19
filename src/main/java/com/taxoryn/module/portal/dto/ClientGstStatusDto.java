package com.taxoryn.module.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client GST Return Status")
public class ClientGstStatusDto {

    private UUID id;
    private String gstin;
    private String returnType;
    private String returnPeriod;
    private String financialYear;
    private LocalDate dueDate;
    private LocalDate filedDate;
    private String status;
    private String arn;
    private BigDecimal totalTaxPayable;
    private BigDecimal itcClaimed;
}
