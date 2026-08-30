package com.taxoryn.module.report.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.report.dto.ClientReportDto;
import com.taxoryn.module.report.dto.FinancialReportDto;
import com.taxoryn.module.report.dto.PracticeOverviewReportDto;
import com.taxoryn.module.report.dto.TaxWorkReportDto;
import com.taxoryn.module.report.dto.WorkManagementReportDto;
import com.taxoryn.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Central Reports", description = "Aggregated Central Reporting APIs across Practice, Tax, Clients, Work, and Financials")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN')")
    @Operation(summary = "Get Practice Overview Report", description = "Consolidated summary of clients, active tax jobs, tasks, compliance, documents, and billing")
    public ResponseEntity<ApiResponse<PracticeOverviewReportDto>> getPracticeOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        PracticeOverviewReportDto report = reportService.getPracticeOverviewReport(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/tax-work")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN')")
    @Operation(summary = "Get Consolidated Tax Work Report", description = "Detailed breakdown of GST, ITR, TDS filings, and statutory compliance obligations")
    public ResponseEntity<ApiResponse<TaxWorkReportDto>> getTaxWork(
            @RequestParam(required = false) String financialYear,
            @RequestParam(required = false) String assessmentYear,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        TaxWorkReportDto report = reportService.getTaxWorkReport(financialYear, assessmentYear, quarter, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN')")
    @Operation(summary = "Get Client Portfolio & Follow-up Report", description = "Client health distribution, pending client actions, and document request pipeline")
    public ResponseEntity<ApiResponse<ClientReportDto>> getClientReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        ClientReportDto report = reportService.getClientReport(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/work")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN')")
    @Operation(summary = "Get Work Management & Team Productivity Report", description = "Task status/priority breakdown and employee workload & completion rate metrics")
    public ResponseEntity<ApiResponse<WorkManagementReportDto>> getWorkManagementReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        WorkManagementReportDto report = reportService.getWorkManagementReport(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'ACCOUNTANT', 'SUPER_ADMIN')")
    @Operation(summary = "Get Financial & Outstanding Invoices Report", description = "Invoicing, collections, realization rate, and aging outstanding invoices list")
    public ResponseEntity<ApiResponse<FinancialReportDto>> getFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        FinancialReportDto report = reportService.getFinancialReport(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
