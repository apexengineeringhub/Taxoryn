package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
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
@Schema(description = "Update ITR Status Workflow Transition Payload")
public class UpdateItrStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target workflow status", example = "UNDER_REVIEW")
    private ItrStatus status;

    @Schema(description = "Optional workflow transition remarks or reason")
    private String notes;

    public UpdateItrStatusRequest(ItrStatus status, String notes) {
        this.status = status;
        this.notes = notes;
    }

    @Schema(description = "Review comments or rework feedback if returning for changes")
    private String reviewComments;

    @Schema(description = "Acknowledgement Number if marking as FILED")
    private String acknowledgementNumber;

    @Schema(description = "Filing Date if marking as FILED")
    private java.time.LocalDate filingDate;

    @Schema(description = "Verification Date if marking as COMPLETED")
    private java.time.LocalDate verificationDate;

    @Schema(description = "Linked Task ID")
    private java.util.UUID taskId;

    @Schema(description = "Linked Compliance ID")
    private java.util.UUID complianceId;

    @Schema(description = "Linked Document Request ID")
    private java.util.UUID documentRequestId;
}
