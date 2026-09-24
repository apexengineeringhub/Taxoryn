package com.taxoryn.module.compliance.service;

import com.taxoryn.module.compliance.entity.ComplianceCycleTemplateEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Service
public class DueDateCalculationServiceImpl implements DueDateCalculationService {

    @Override
    public LocalDate calculateStatutoryDueDate(
            ComplianceCycleTemplateEntity template,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String financialYear,
            String assessmentYear
    ) {
        if (template == null) {
            return periodEndDate != null ? periodEndDate : LocalDate.now().plusDays(30);
        }

        Integer dueDay = template.getDefaultDueDay();
        Integer monthOffset = template.getDefaultDueMonthOffset() != null ? template.getDefaultDueMonthOffset() : 1;
        Integer fixedMonth = template.getFixedDueMonth();

        return switch (template.getRecurrenceType()) {
            case MONTHLY -> {
                LocalDate base = periodEndDate != null ? periodEndDate : (periodStartDate != null ? periodStartDate : LocalDate.now());
                YearMonth targetYm = YearMonth.from(base).plusMonths(monthOffset);
                int day = dueDay != null ? Math.min(dueDay, targetYm.lengthOfMonth()) : targetYm.lengthOfMonth();
                yield targetYm.atDay(day);
            }
            case QUARTERLY -> {
                if (fixedMonth != null && dueDay != null) {
                    int year = resolveYearFromPeriodOrCurrent(periodEndDate, financialYear, assessmentYear);
                    YearMonth targetYm = YearMonth.of(year, fixedMonth);
                    yield targetYm.atDay(Math.min(dueDay, targetYm.lengthOfMonth()));
                } else {
                    LocalDate base = periodEndDate != null ? periodEndDate : LocalDate.now();
                    YearMonth targetYm = YearMonth.from(base).plusMonths(monthOffset);
                    int day = dueDay != null ? Math.min(dueDay, targetYm.lengthOfMonth()) : targetYm.lengthOfMonth();
                    yield targetYm.atDay(day);
                }
            }
            case ANNUALLY -> {
                int year = resolveYearFromPeriodOrCurrent(periodEndDate, financialYear, assessmentYear);
                int month = fixedMonth != null ? fixedMonth : 7; // default July
                int day = dueDay != null ? dueDay : 31;
                YearMonth targetYm = YearMonth.of(year, month);
                yield targetYm.atDay(Math.min(day, targetYm.lengthOfMonth()));
            }
            case ONE_TIME, EVENT_BASED -> periodEndDate != null ? periodEndDate : LocalDate.now().plusDays(15);
        };
    }

    @Override
    public LocalDate calculateInternalTargetDate(LocalDate statutoryDueDate, Integer offsetDays) {
        if (statutoryDueDate == null) {
            return null;
        }
        int offset = (offsetDays != null && offsetDays > 0) ? offsetDays : 3;
        return statutoryDueDate.minusDays(offset);
    }

    private int resolveYearFromPeriodOrCurrent(LocalDate periodEndDate, String financialYear, String assessmentYear) {
        if (assessmentYear != null && assessmentYear.contains("-")) {
            try {
                String firstPart = assessmentYear.split("-")[0].trim();
                return Integer.parseInt(firstPart);
            } catch (Exception ignored) {}
        }
        if (financialYear != null && financialYear.contains("-")) {
            try {
                String firstPart = financialYear.split("-")[0].trim();
                return Integer.parseInt(firstPart) + 1;
            } catch (Exception ignored) {}
        }
        if (periodEndDate != null) {
            return periodEndDate.getYear();
        }
        return LocalDate.now().getYear();
    }
}
