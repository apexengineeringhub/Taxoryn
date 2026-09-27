package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "360-Degree Unified Client Foundation Read Model")
public class Client360Dto {

    @Schema(description = "Client master basic information")
    private ClientDto client;

    @Schema(description = "Client tax & statutory identifiers")
    private ClientOverviewDto.StatutoryDetails identifiers;

    @Schema(description = "Primary servicing location")
    private ClientLocationAssignmentDto primaryLocation;

    @Schema(description = "All assigned branch/operating locations")
    private List<ClientLocationAssignmentDto> locations;

    @Schema(description = "Assigned practitioners and staff users (portfolio)")
    private List<ClientUserAssignmentDto> assignedUsers;

    @Schema(description = "Active and configured service engagements")
    private List<ClientServiceDto> services;

    @Schema(description = "Client GST registrations (when GST module is enabled)")
    private List<com.taxoryn.module.gst.dto.GstRegistrationDto> gstRegistrations;

    @Schema(description = "Client ITR profile (when ITR module is enabled)")
    private com.taxoryn.module.itr.dto.ItrProfileDto itrProfile;

    @Schema(description = "Client lifecycle status")
    private ClientStatus status;
}
