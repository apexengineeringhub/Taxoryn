package com.taxoryn.module.report.service;

import com.taxoryn.module.report.dto.ClientReportDto;
import com.taxoryn.module.report.dto.FinancialReportDto;
import com.taxoryn.module.report.dto.PracticeOverviewReportDto;
import com.taxoryn.module.report.dto.TaxWorkReportDto;
import com.taxoryn.module.report.dto.WorkManagementReportDto;

import java.time.LocalDate;

public interface ReportService {

    PracticeOverviewReportDto getPracticeOverviewReport(LocalDate fromDate, LocalDate toDate);

    TaxWorkReportDto getTaxWorkReport(String financialYear, String assessmentYear, String quarter, LocalDate fromDate, LocalDate toDate);

    ClientReportDto getClientReport(LocalDate fromDate, LocalDate toDate);

    WorkManagementReportDto getWorkManagementReport(LocalDate fromDate, LocalDate toDate);

    FinancialReportDto getFinancialReport(LocalDate fromDate, LocalDate toDate);
}
