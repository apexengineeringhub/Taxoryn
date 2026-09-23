package com.taxoryn.module.moduleconfig.aspect;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.service.ModuleEntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class RequiresModuleAspect {

    private final ModuleEntitlementService moduleEntitlementService;

    @Before("@within(com.taxoryn.module.moduleconfig.annotation.RequiresModule) || @annotation(com.taxoryn.module.moduleconfig.annotation.RequiresModule)")
    public void enforceModuleRequirement(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. Resolve annotation from method first, then enclosing class
        RequiresModule annotation = AnnotationUtils.findAnnotation(method, RequiresModule.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequiresModule.class);
        }

        if (annotation == null) {
            return;
        }

        UUID organizationId = com.taxoryn.core.security.TenantContext.getTenantId();
        if (organizationId == null) {
            organizationId = SecurityUtils.getCurrentUser().map(SecurityUser::getOrganizationId).orElse(null);
        }
        if (organizationId == null) {
            // Unauthenticated or no tenant context (handled by SecurityUtils / Spring Security)
            return;
        }

        moduleEntitlementService.verifyModuleAndSubscriptionAccess(organizationId, annotation.value());
    }
}
