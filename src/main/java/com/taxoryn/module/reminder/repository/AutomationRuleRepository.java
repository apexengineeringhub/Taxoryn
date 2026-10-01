package com.taxoryn.module.reminder.repository;

import com.taxoryn.module.reminder.entity.AutomationEventType;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRuleEntity, UUID> {

    Optional<AutomationRuleEntity> findById(UUID id);

    // -------------------------------------------------------------------------
    // Organization-scoped queries (for Settings → Automations page)
    // -------------------------------------------------------------------------

    List<AutomationRuleEntity> findAllByOrganizationIdOrderByEventTypeAscCreatedAtAsc(UUID organizationId);

    List<AutomationRuleEntity> findAllByOrganizationIdAndEnabledOrderByEventTypeAsc(UUID organizationId, boolean enabled);

    // -------------------------------------------------------------------------
    // Automation processor — find rules applicable to an event for an org
    //
    // An applicable rule is either:
    //   a) a rule belonging to the organization (org-specific override), OR
    //   b) a system-default rule (organizationId IS NULL)
    // BUT if an org-specific rule exists for the same event, system defaults
    // should NOT fire for that org (org rules take precedence).
    //
    // For P0.5 simplicity: return ALL matching rules (both org + system defaults)
    // and let the AutomationService decide precedence.
    // -------------------------------------------------------------------------

    /**
     * Returns all enabled automation rules for a specific event type that are
     * applicable to the given organization — i.e. org-specific OR system-default (null org).
     */
    @Query("""
            SELECT r FROM AutomationRuleEntity r
            WHERE r.eventType = :eventType
              AND r.enabled = true
              AND (r.organizationId = :organizationId OR r.organizationId IS NULL)
            ORDER BY r.organizationId DESC NULLS LAST
            """)
    List<AutomationRuleEntity> findEnabledByEventTypeForOrganization(
            @Param("eventType") AutomationEventType eventType,
            @Param("organizationId") UUID organizationId);

    /**
     * Returns all enabled system-default rules for a given event type (organizationId IS NULL).
     */
    List<AutomationRuleEntity> findAllByEventTypeAndEnabledTrueAndOrganizationIdIsNull(AutomationEventType eventType);

    // -------------------------------------------------------------------------
    // Admin — list all system defaults
    // -------------------------------------------------------------------------

    List<AutomationRuleEntity> findAllByOrganizationIdIsNullOrderByEventTypeAsc();

    // -------------------------------------------------------------------------
    // Existence check — avoid duplicates when an org creates org-specific rules
    // -------------------------------------------------------------------------

    boolean existsByOrganizationIdAndEventType(UUID organizationId, AutomationEventType eventType);
}
