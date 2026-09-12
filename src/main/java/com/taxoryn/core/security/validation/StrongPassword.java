package com.taxoryn.core.security.validation;

import com.taxoryn.core.security.PasswordSecurityUtils;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a password satisfies the canonical Taxoryn strong-password policy:
 * - Minimum 12 characters (maximum 100 characters)
 * - Contains at least one uppercase letter (A-Z)
 * - Contains at least one lowercase letter (a-z)
 * - Contains at least one digit (0-9)
 * - Contains at least one special character
 * - Is not in the known weak / default password list
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default PasswordSecurityUtils.PASSWORD_REQUIREMENTS_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * When true, null, empty, or whitespace-only values pass validation.
     * Useful for optional password fields (e.g. optional temporary password or first-time setup alias).
     */
    boolean optional() default false;
}
