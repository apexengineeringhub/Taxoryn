package com.taxoryn.module.moduleconfig.annotation;

import com.taxoryn.module.moduleconfig.model.ProductModuleCode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that an endpoint or service requires a specific product module to be enabled
 * and commercially entitled via an active subscription.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresModule {

    /**
     * The product module code that must be enabled and entitled.
     */
    ProductModuleCode value();
}
