package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeDashboardStatsDto {
    private long totalActiveNotices;
    private long overdueNotices;
    private long dueTodayNotices;
    private long dueThisWeekNotices;
    private long pendingReviewNotices;
    private long pendingPartnerApprovalNotices;
    private long upcomingHearingsCount;
    private long criticalPriorityCount;
    private long resolvedThisMonthCount;
    private BigDecimal totalDemandUnderDispute;
    private Map<NoticeDepartment, Long> byDepartment;
    private Map<NoticeStatus, Long> byStatus;
}
