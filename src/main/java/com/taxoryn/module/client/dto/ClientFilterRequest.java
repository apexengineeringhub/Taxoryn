package com.taxoryn.module.client.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Client Search, Filter, and Pagination Parameters")
public class ClientFilterRequest extends PageRequestDto {

    @Schema(description = "Search term across name, legal name, PAN, GSTIN, TAN, email, phone", example = "Zenith")
    private String search;

    @Schema(description = "Filter by client entity type", example = "PRIVATE_LIMITED")
    private ClientType clientType;

    @Schema(description = "Filter by status", example = "ACTIVE")
    private ClientStatus status;

    @Schema(description = "Filter by assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Filter by city", example = "Mumbai")
    private String city;

    @Schema(description = "Filter by state", example = "Maharashtra")
    private String state;

    @Schema(description = "Filter by PAN", example = "AAACZ1234D")
    private String pan;

    @Schema(description = "Filter by GSTIN", example = "27AAACZ1234D1Z8")
    private String gstin;
}
