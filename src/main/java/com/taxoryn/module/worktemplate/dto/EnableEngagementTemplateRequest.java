package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.worktemplate.model.RecurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnableEngagementTemplateRequest {

    private RecurrenceType recurrenceType;
    private Integer recurrenceInterval;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    private LocalDate startDate;
    private LocalDate endDate;
}
