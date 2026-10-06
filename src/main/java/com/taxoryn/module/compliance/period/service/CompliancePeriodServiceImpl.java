package com.taxoryn.module.compliance.period.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.module.compliance.obligation.util.PeriodValidator;
import com.taxoryn.module.compliance.period.model.CompliancePeriod;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

@Slf4j
@Service
public class CompliancePeriodServiceImpl implements CompliancePeriodService {

    @Override
    public CompliancePeriod resolvePeriod(CompliancePeriodType periodType, String periodKey) {
        PeriodValidator.validatePeriodKey(periodType, periodKey);
        String trimmed = periodKey.trim();

        return switch (periodType) {
            case MONTH -> resolveMonth(trimmed);
            case QUARTER -> resolveQuarter(trimmed);
            case HALF_YEAR -> resolveHalfYear(trimmed);
            case FINANCIAL_YEAR -> resolveFinancialYear(trimmed);
            case ASSESSMENT_YEAR -> resolveAssessmentYear(trimmed);
            case EVENT -> resolveEvent(trimmed);
        };
    }

    private CompliancePeriod resolveMonth(String periodKey) {
        YearMonth ym = YearMonth.parse(periodKey);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        int year = ym.getYear();
        int month = ym.getMonthValue();
        int fyStartYear = (month >= 4) ? year : (year - 1);
        String fy = formatFy(fyStartYear);
        String ay = formatAy(fyStartYear);

        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String label = monthName + " " + year;

        return CompliancePeriod.builder()
                .periodType(CompliancePeriodType.MONTH)
                .periodKey(periodKey)
                .startDate(start)
                .endDate(end)
                .financialYear(fy)
                .assessmentYear(ay)
                .displayLabel(label)
                .build();
    }

    private CompliancePeriod resolveQuarter(String periodKey) {
        int qIndex = periodKey.lastIndexOf("Q");
        int qNum = Integer.parseInt(periodKey.substring(qIndex + 1));
        int fyStartYear = Integer.parseInt(periodKey.substring(0, 4));

        LocalDate start;
        LocalDate end;
        String qMonths;

        switch (qNum) {
            case 1 -> {
                start = LocalDate.of(fyStartYear, Month.APRIL, 1);
                end = LocalDate.of(fyStartYear, Month.JUNE, 30);
                qMonths = "Apr - Jun";
            }
            case 2 -> {
                start = LocalDate.of(fyStartYear, Month.JULY, 1);
                end = LocalDate.of(fyStartYear, Month.SEPTEMBER, 30);
                qMonths = "Jul - Sep";
            }
            case 3 -> {
                start = LocalDate.of(fyStartYear, Month.OCTOBER, 1);
                end = LocalDate.of(fyStartYear, Month.DECEMBER, 31);
                qMonths = "Oct - Dec";
            }
            case 4 -> {
                start = LocalDate.of(fyStartYear + 1, Month.JANUARY, 1);
                end = LocalDate.of(fyStartYear + 1, Month.MARCH, 31);
                qMonths = "Jan - Mar";
            }
            default -> throw new BadRequestException("Invalid quarter number: " + qNum);
        }

        String fy = formatFy(fyStartYear);
        String ay = formatAy(fyStartYear);
        String label = "Q" + qNum + " (" + qMonths + ") FY " + fy;

        return CompliancePeriod.builder()
                .periodType(CompliancePeriodType.QUARTER)
                .periodKey(periodKey)
                .startDate(start)
                .endDate(end)
                .financialYear(fy)
                .assessmentYear(ay)
                .displayLabel(label)
                .build();
    }

    private CompliancePeriod resolveHalfYear(String periodKey) {
        int hIndex = periodKey.lastIndexOf("H");
        int hNum = Integer.parseInt(periodKey.substring(hIndex + 1));
        int fyStartYear = Integer.parseInt(periodKey.substring(0, 4));

        LocalDate start;
        LocalDate end;
        String hMonths;

        if (hNum == 1) {
            start = LocalDate.of(fyStartYear, Month.APRIL, 1);
            end = LocalDate.of(fyStartYear, Month.SEPTEMBER, 30);
            hMonths = "Apr - Sep";
        } else if (hNum == 2) {
            start = LocalDate.of(fyStartYear, Month.OCTOBER, 1);
            end = LocalDate.of(fyStartYear + 1, Month.MARCH, 31);
            hMonths = "Oct - Mar";
        } else {
            throw new BadRequestException("Invalid half-year number: " + hNum);
        }

        String fy = formatFy(fyStartYear);
        String ay = formatAy(fyStartYear);
        String label = "H" + hNum + " (" + hMonths + ") FY " + fy;

        return CompliancePeriod.builder()
                .periodType(CompliancePeriodType.HALF_YEAR)
                .periodKey(periodKey)
                .startDate(start)
                .endDate(end)
                .financialYear(fy)
                .assessmentYear(ay)
                .displayLabel(label)
                .build();
    }

    private CompliancePeriod resolveFinancialYear(String periodKey) {
        int fyStartYear = Integer.parseInt(periodKey.substring(0, 4));
        LocalDate start = LocalDate.of(fyStartYear, Month.APRIL, 1);
        LocalDate end = LocalDate.of(fyStartYear + 1, Month.MARCH, 31);

        String fy = formatFy(fyStartYear);
        String ay = formatAy(fyStartYear);
        String label = "FY " + fy;

        return CompliancePeriod.builder()
                .periodType(CompliancePeriodType.FINANCIAL_YEAR)
                .periodKey(periodKey)
                .startDate(start)
                .endDate(end)
                .financialYear(fy)
                .assessmentYear(ay)
                .displayLabel(label)
                .build();
    }

    private CompliancePeriod resolveAssessmentYear(String periodKey) {
        int ayStartYear = Integer.parseInt(periodKey.substring(0, 4));
        int fyStartYear = ayStartYear - 1;

        LocalDate start = LocalDate.of(fyStartYear, Month.APRIL, 1);
        LocalDate end = LocalDate.of(fyStartYear + 1, Month.MARCH, 31);

        String fy = formatFy(fyStartYear);
        String ay = formatAy(fyStartYear);
        String label = "AY " + ay + " (FY " + fy + ")";

        return CompliancePeriod.builder()
                .periodType(CompliancePeriodType.ASSESSMENT_YEAR)
                .periodKey(periodKey)
                .startDate(start)
                .endDate(end)
                .financialYear(fy)
                .assessmentYear(ay)
                .displayLabel(label)
                .build();
    }

    private CompliancePeriod resolveEvent(String periodKey) {
        String datePart = periodKey.contains(":") ? periodKey.split(":")[0] : periodKey;
        LocalDate eventDate = LocalDate.parse(datePart);

        int year = eventDate.getYear();
        int month = eventDate.getMonthValue();
        int fyStartYear = (month >= 4) ? year : (year - 1);

        String fy = formatFy(fyStartYear);
        String ay = formatAy(fyStartYear);
        String label = "Event " + periodKey;

        return CompliancePeriod.builder()
                .periodType(CompliancePeriodType.EVENT)
                .periodKey(periodKey)
                .startDate(eventDate)
                .endDate(eventDate)
                .financialYear(fy)
                .assessmentYear(ay)
                .displayLabel(label)
                .build();
    }

    private String formatFy(int fyStartYear) {
        int endTwoDigit = (fyStartYear + 1) % 100;
        return fyStartYear + "-" + String.format("%02d", endTwoDigit);
    }

    private String formatAy(int fyStartYear) {
        int ayStartYear = fyStartYear + 1;
        int endTwoDigit = (ayStartYear + 1) % 100;
        return ayStartYear + "-" + String.format("%02d", endTwoDigit);
    }
}
