package com.taxoryn.module.compliance.calendar.service;

import com.taxoryn.module.compliance.calendar.model.DeadlineStatus;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Deterministic classifier for compliance deadlines based on statutory due dates and reference business dates.
 */
@Component
public class ComplianceDeadlineClassifier {

    public record ClassificationResult(
            DeadlineStatus status,
            long daysRemaining,
            long daysOverdue
    ) {}

    /**
     * Classifies a compliance obligation against a reference business date.
     *
     * @param obligation    the compliance obligation entity
     * @param referenceDate reference date (defaults to current date if null)
     * @return deterministic ClassificationResult
     */
    public ClassificationResult classify(ComplianceObligationEntity obligation, LocalDate referenceDate) {
        if (obligation == null) {
            return new ClassificationResult(DeadlineStatus.NO_DUE_DATE, 0, 0);
        }

        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();

        // 1. Terminal states check (Excluded from active radar)
        if (obligation.getStatus() == ComplianceObligationStatus.COMPLETED) {
            return new ClassificationResult(DeadlineStatus.COMPLETED, 0, 0);
        }
        if (obligation.getStatus() == ComplianceObligationStatus.CANCELLED) {
            return new ClassificationResult(DeadlineStatus.CANCELLED, 0, 0);
        }

        // 2. Authoritative due date resolution (statutoryDueDate prioritized over legacy dueDate)
        LocalDate dueDate = obligation.getStatutoryDueDate() != null
                ? obligation.getStatutoryDueDate()
                : obligation.getDueDate();

        if (dueDate == null) {
            return new ClassificationResult(DeadlineStatus.NO_DUE_DATE, 0, 0);
        }

        // 3. Overdue (due date strictly prior to reference date)
        if (dueDate.isBefore(refDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate, refDate);
            return new ClassificationResult(DeadlineStatus.OVERDUE, 0, overdueDays);
        }

        // 4. Due Today (exact match with reference date)
        if (dueDate.isEqual(refDate)) {
            return new ClassificationResult(DeadlineStatus.DUE_TODAY, 0, 0);
        }

        // 5. Due Tomorrow (reference date + 1 day)
        LocalDate tomorrow = refDate.plusDays(1);
        if (dueDate.isEqual(tomorrow)) {
            return new ClassificationResult(DeadlineStatus.DUE_TOMORROW, 1, 0);
        }

        // 6. Due Within 3 Days (reference date + 2 or + 3 days)
        LocalDate plusThreeDays = refDate.plusDays(3);
        if (!dueDate.isAfter(plusThreeDays)) {
            long remaining = ChronoUnit.DAYS.between(refDate, dueDate);
            return new ClassificationResult(DeadlineStatus.DUE_WITHIN_3_DAYS, remaining, 0);
        }

        // 7. Due This Week (up to end of current ISO week - Sunday)
        LocalDate endOfIsoWeek = refDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        if (!dueDate.isAfter(endOfIsoWeek)) {
            long remaining = ChronoUnit.DAYS.between(refDate, dueDate);
            return new ClassificationResult(DeadlineStatus.DUE_THIS_WEEK, remaining, 0);
        }

        // 8. Upcoming (future beyond the current week)
        long remaining = ChronoUnit.DAYS.between(refDate, dueDate);
        return new ClassificationResult(DeadlineStatus.UPCOMING, remaining, 0);
    }
}
