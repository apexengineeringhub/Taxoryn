package com.taxoryn.module.reminder.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.reminder.dto.AutomationRuleDto;
import com.taxoryn.module.reminder.dto.SaveAutomationRuleRequest;
import com.taxoryn.module.reminder.entity.AutomationActionType;
import com.taxoryn.module.reminder.entity.AutomationEventType;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import com.taxoryn.module.reminder.entity.AutomationTargetType;
import com.taxoryn.module.reminder.repository.AutomationRuleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationServiceTest {

    @Mock
    private AutomationRuleRepository ruleRepository;

    @InjectMocks
    private AutomationServiceImpl automationService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@taxoryn.com")
                .roles(Set.of("ORG_ADMIN"))
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should list both org-specific and system-default rules")
    void testListRules() {
        AutomationRuleEntity orgRule = AutomationRuleEntity.builder()
                .organizationId(tenantId)
                .name("Org Task Rule")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-1)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build();
        orgRule.setId(UUID.randomUUID());

        AutomationRuleEntity systemRule = AutomationRuleEntity.builder()
                .organizationId(null)
                .name("Default System Rule")
                .eventType(AutomationEventType.TASK_OVERDUE)
                .daysOffset(0)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build();
        systemRule.setId(UUID.randomUUID());

        when(ruleRepository.findAllByOrganizationIdOrderByEventTypeAscCreatedAtAsc(tenantId))
                .thenReturn(List.of(orgRule));
        when(ruleRepository.findAllByOrganizationIdIsNullOrderByEventTypeAsc())
                .thenReturn(List.of(systemRule));

        List<AutomationRuleDto> result = automationService.listRules();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Org Task Rule", result.get(0).getName());
        assertEquals("Default System Rule", result.get(1).getName());
        assertTrue(result.get(1).isSystemDefault());
    }

    @Test
    @DisplayName("Should create org-specific automation rule")
    void testCreateRule() {
        SaveAutomationRuleRequest request = SaveAutomationRuleRequest.builder()
                .name("3 Days Before Due Date")
                .description("Remind assignee 3 days in advance")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-3)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build();

        when(ruleRepository.save(any(AutomationRuleEntity.class))).thenAnswer(inv -> {
            AutomationRuleEntity r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        AutomationRuleDto result = automationService.createRule(request);

        assertNotNull(result);
        assertEquals("3 Days Before Due Date", result.getName());
        assertEquals(-3, result.getDaysOffset());
        assertEquals(tenantId, result.getOrganizationId());
        assertFalse(result.isSystemDefault());

        ArgumentCaptor<AutomationRuleEntity> captor = ArgumentCaptor.forClass(AutomationRuleEntity.class);
        verify(ruleRepository).save(captor.capture());
        assertEquals(tenantId, captor.getValue().getOrganizationId());
        assertEquals(AutomationActionType.CREATE_REMINDER, captor.getValue().getActionType());
    }

    @Test
    @DisplayName("Should update org-specific rule successfully")
    void testUpdateRule() {
        UUID ruleId = UUID.randomUUID();
        AutomationRuleEntity existing = AutomationRuleEntity.builder()
                .organizationId(tenantId)
                .name("Old Name")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-1)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build();
        existing.setId(ruleId);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(AutomationRuleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveAutomationRuleRequest request = SaveAutomationRuleRequest.builder()
                .name("Updated Name")
                .eventType(AutomationEventType.TASK_OVERDUE)
                .daysOffset(0)
                .targetType(AutomationTargetType.ENGAGEMENT_OWNER)
                .enabled(false)
                .build();

        AutomationRuleDto result = automationService.updateRule(ruleId, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals(AutomationEventType.TASK_OVERDUE, result.getEventType());
        assertEquals("ENGAGEMENT_OWNER", result.getTargetType());
        assertFalse(result.getEnabled());
    }

    @Test
    @DisplayName("Should reject updating or deleting system default rules")
    void testSystemDefaultRuleModificationRejected() {
        UUID ruleId = UUID.randomUUID();
        AutomationRuleEntity systemRule = AutomationRuleEntity.builder()
                .organizationId(null) // System default
                .name("System Default")
                .eventType(AutomationEventType.TASK_DUE)
                .build();
        systemRule.setId(ruleId);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(systemRule));

        SaveAutomationRuleRequest request = SaveAutomationRuleRequest.builder()
                .name("Try To Change")
                .build();

        assertThrows(IllegalStateException.class, () -> automationService.updateRule(ruleId, request));
        assertThrows(IllegalStateException.class, () -> automationService.deleteRule(ruleId));
        assertThrows(IllegalStateException.class, () -> automationService.enableRule(ruleId));
        assertThrows(IllegalStateException.class, () -> automationService.disableRule(ruleId));
    }

    @Test
    @DisplayName("Should prevent cross-tenant rule modification")
    void testCrossTenantRuleModificationForbidden() {
        UUID ruleId = UUID.randomUUID();
        UUID otherOrg = UUID.randomUUID();

        AutomationRuleEntity otherOrgRule = AutomationRuleEntity.builder()
                .organizationId(otherOrg)
                .name("Other Org Rule")
                .eventType(AutomationEventType.TASK_DUE)
                .build();
        otherOrgRule.setId(ruleId);

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(otherOrgRule));

        SaveAutomationRuleRequest request = SaveAutomationRuleRequest.builder()
                .name("Hacked Name")
                .build();

        assertThrows(AccessDeniedException.class, () -> automationService.updateRule(ruleId, request));
        assertThrows(AccessDeniedException.class, () -> automationService.deleteRule(ruleId));
    }
}
