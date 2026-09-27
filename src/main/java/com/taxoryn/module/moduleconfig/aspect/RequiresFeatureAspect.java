package com.taxoryn.module.moduleconfig.aspect;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.moduleconfig.annotation.RequiresFeature;
import com.taxoryn.module.moduleconfig.service.FeatureConfigurationService;
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
@Order(11)
@RequiredArgsConstructor
public class RequiresFeatureAspect {

    private final FeatureConfigurationService featureConfigurationService;

    @Before("@within(com.taxoryn.module.moduleconfig.annotation.RequiresFeature) || @annotation(com.taxoryn.module.moduleconfig.annotation.RequiresFeature)")
    public void enforceFeatureRequirement(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresFeature annotation = AnnotationUtils.findAnnotation(method, RequiresFeature.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequiresFeature.class);
        }

        if (annotation == null) {
            return;
        }

        UUID organizationId = TenantContext.getTenantId();
        if (organizationId == null) {
            organizationId = SecurityUtils.getCurrentUser().map(SecurityUser::getOrganizationId).orElse(null);
        }
        if (organizationId == null) {
            return;
        }

        featureConfigurationService.verifyFeatureAccess(organizationId, annotation.module(), annotation.feature());
    }
}
