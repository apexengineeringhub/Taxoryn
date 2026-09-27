package com.taxoryn.module.subscription.entity;

import com.taxoryn.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_plan_modules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanModuleEntity extends AuditableEntity {

    @Column(name = "plan_code", nullable = false, length = 50)
    private String planCode;

    @Column(name = "module_code", nullable = false, length = 50)
    private String moduleCode;

    @Column(name = "is_included", nullable = false)
    @Builder.Default
    private boolean isIncluded = true;
}
