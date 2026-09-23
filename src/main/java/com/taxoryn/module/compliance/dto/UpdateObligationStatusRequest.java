package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Compliance Obligation Status Payload")
public class UpdateObligationStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New compliance status", example = "COMPLETED")
    private ComplianceObligationStatus status;

    @Schema(description = "Optional status transition notes")
    private String notes;

    public String getRemarks() {
        return notes;
    }

    public void setRemarks(String remarks) {
        this.notes = remarks;
    }

    public static class UpdateObligationStatusRequestBuilder {
        public UpdateObligationStatusRequestBuilder status(ComplianceObligationStatus status) {
            this.status = status;
            return this;
        }

        public UpdateObligationStatusRequestBuilder remarks(String remarks) {
            this.notes = remarks;
            return this;
        }

        public UpdateObligationStatusRequestBuilder status(com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus legacyStatus) {
            if (legacyStatus != null) {
                this.status = switch (legacyStatus) {
                    case PENDING -> ComplianceObligationStatus.UPCOMING;
                    case IN_PROGRESS -> ComplianceObligationStatus.IN_PROGRESS;
                    case COMPLETED -> ComplianceObligationStatus.COMPLETED;
                    case OVERDUE -> ComplianceObligationStatus.OVERDUE;
                    case WAIVED, CANCELLED -> ComplianceObligationStatus.CANCELLED;
                };
            }
            return this;
        }
    }
}
