package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxNoticeFilterRequest {
    private String search;
    private UUID clientId;
    private NoticeDepartment department;
    private NoticeStatus status;
    private NoticePriority priority;
    private UUID assignedEmployeeId;
    private UUID reviewerEmployeeId;
    private UUID partnerEmployeeId;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    private Boolean overdueOnly;
    private Boolean upcomingHearing;
}
