package com.taxoryn.module.client.dto;

import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight immutable client context summary for cross-module integration.
 * Exposes core client identity, tenancy, classification, and statutory numbers
 * without exposing internal JPA entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lightweight Client Context Summary for cross-module business operations")
public class ClientContextSummaryDto {

    @Schema(description = "Unique client ID")
    private UUID clientId;

    @Schema(description = "Owning organization / tenant ID")
    private UUID organizationId;

    @Schema(description = "Client display name")
    private String displayName;

    @Schema(description = "Client legal registered name")
    private String legalName;

    @Schema(description = "Client trade name")
    private String tradeName;

    @Schema(description = "Internal unique practice client code")
    private String clientCode;

    @Schema(description = "Constitution / entity classification type")
    private ClientType clientType;

    @Schema(description = "Current lifecycle status")
    private ClientStatus status;

    @Schema(description = "Timestamp of last status transition")
    private java.time.Instant statusChangedAt;

    @Schema(description = "Business reason for the last status transition")
    private String statusChangeReason;

    @Schema(description = "Whether the client is currently active and eligible for compliance processing")
    private boolean active;

    @Schema(description = "Permanent Account Number (PAN)")
    private String pan;

    @Schema(description = "Primary GSTIN")
    private String gstin;

    @Schema(description = "Tax Deduction and Collection Account Number (TAN)")
    private String tan;

    @Schema(description = "Corporate Identification Number (CIN)")
    private String cin;

    @Schema(description = "Primary contact email")
    private String email;

    @Schema(description = "Primary contact phone")
    private String phone;

    @Schema(description = "Business activity description")
    private String businessActivity;

    @Schema(description = "Industry or sector")
    private String industry;

    @Schema(description = "Business scale (MICRO, SMALL, MEDIUM, LARGE, INDIVIDUAL)")
    private String businessScale;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State")
    private String state;

    @Schema(description = "2-digit GST state code")
    private String stateCode;

    @Schema(description = "PIN Code")
    private String pincode;

    @Schema(description = "Primary practice location ID")
    private UUID locationId;

    @Schema(description = "Assigned primary practitioner / employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Count of active service engagements")
    private Long activeServicesCount;

    @Schema(description = "Primary contact ID")
    private UUID primaryContactId;

    @Schema(description = "Primary branch ID")
    private UUID primaryBranchId;

    @Schema(description = "Total contacts count")
    private Long contactsCount;

    @Schema(description = "Total branches count")
    private Long branchesCount;

    @Schema(description = "Total relationships count")
    private Long relationshipsCount;

    @Schema(description = "Client profile completeness assessment")
    private ClientProfileCompletenessDto completeness;

    @Schema(description = "Count of active attention signals requiring review")
    private Integer attentionSignalsCount;

    @Schema(description = "Count of high or critical priority attention signals")
    private Integer highPrioritySignalsCount;
}
