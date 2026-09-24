package com.taxoryn.module.compliance.service;

import com.taxoryn.module.compliance.entity.ComplianceCycleTemplateEntity;

import java.time.LocalDate;

public interface DueDateCalculationService {

    LocalDate calculateStatutoryDueDate(
            ComplianceCycleTemplateEntity template,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String financialYear,
            String assessmentYear
    );

    LocalDate calculateInternalTargetDate(LocalDate statutoryDueDate, Integer offsetDays);
}
