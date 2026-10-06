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

    @Schema(description = "Client TDS profile (when TDS module is enabled)")
    private com.taxoryn.module.tds.dto.TdsProfileDto tdsProfile;

    @Schema(description = "Client tax notices (when Tax Notices module is enabled)")
    private List<com.taxoryn.module.notice.dto.TaxNoticeDto> taxNotices;

    @Schema(description = "Client billing history & balance (when Billing module is enabled)")
    private com.taxoryn.module.billing.dto.ClientBillingHistoryDto billing;

    @Schema(description = "Client lifecycle status")
    private ClientStatus status;

    @Schema(description = "Timestamp of last status transition")
    private java.time.Instant statusChangedAt;

    @Schema(description = "Business reason for the last status transition")
    private String statusChangeReason;

    @Schema(description = "Comprehensive Client Business Profile")
    private ClientProfileDto profile;

    @Schema(description = "Client profile completeness assessment")
    private ClientProfileCompletenessDto completeness;

    @Schema(description = "Primary client contact person")
    private ClientContactDto primaryContact;

    @Schema(description = "Total contacts count")
    private Long contactsCount;

    @Schema(description = "Active contacts count")
    private Long activeContactsCount;

    @Schema(description = "Primary client branch / registered office")
    private ClientBranchDto primaryBranch;

    @Schema(description = "Total client branches count")
    private Long branchesCount;

    @Schema(description = "Active client branches count")
    private Long activeBranchesCount;

    @Schema(description = "Total related clients / group relationships count")
    private Long relationshipsCount;

    @Schema(description = "Deterministic intelligence summary and recommendations")
    private ClientIntelligenceSummaryDto intelligenceSummary;

    @Schema(description = "Recent activity & timeline entries projection")
    private List<ClientTimelineItemDto> recentTimeline;

    @Schema(description = "Client Workflow Tasks Analytics / Work Summary")
    private ClientOverviewDto.ClientTaskSummary taskSummary;

    @Schema(description = "Compliance & Filing Summary (GST, ITR, TDS, Accounting)")
    private ClientOverviewDto.ClientComplianceSummary complianceSummary;

    @Schema(description = "Client Compliance Profile Summary (Phase 29.1)")
    private com.taxoryn.module.compliance.profile.dto.ComplianceProfileSummaryDto complianceProfile;

    @Schema(description = "Client Compliance Applicability Summary (Phase 29.3)")
    private com.taxoryn.module.compliance.applicability.dto.ComplianceApplicabilitySummaryDto complianceApplicability;

    @Schema(description = "Client Compliance Obligations Summary (Phase 29.4)")
    private com.taxoryn.module.compliance.obligation.dto.ComplianceObligationSummaryDto complianceObligations;

    @Schema(description = "Documents Vault Summary")
    private ClientOverviewDto.ClientDocumentSummary documentsSummary;

    @Schema(description = "Client Document Requests Summary")
    private ClientOverviewDto.ClientDocRequestSummary docRequestsSummary;

    @Schema(description = "Billing & Financial Summary (Redacted for non-billing staff)")
    private ClientOverviewDto.ClientBillingSummary billingSummary;

    @Schema(description = "Tax Notices & Litigation Summary")
    private ClientOverviewDto.ClientNoticeSummary noticeSummary;

    @Schema(description = "Recent Communication History & Interaction Notes")
    private List<ClientNoteDto> recentNotes;

    @Schema(description = "Chronological Unified Activity Timeline")
    private List<ClientOverviewDto.ClientActivityItem> activityTimeline;

    @Schema(description = "Client engagements portfolio")
    private List<com.taxoryn.module.engagement.dto.EngagementDto> engagements;

    @Schema(description = "Key client contacts (top active/primary)")
    private List<ClientContactDto> keyContacts;

    @Schema(description = "Key client branches (top active/primary)")
    private List<ClientBranchDto> keyBranches;
}
