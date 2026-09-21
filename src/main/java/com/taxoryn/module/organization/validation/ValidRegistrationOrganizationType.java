package com.taxoryn.module.organization.validation;

import com.taxoryn.module.organization.entity.OrganizationType;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that an OrganizationType provided during new organization registration is valid,
 * non-null, and NOT UNKNOWN (which is reserved for legacy or unclassified organizations).
 */
@Documented
@Constraint(validatedBy = ValidRegistrationOrganizationType.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRegistrationOrganizationType {

    String message() default "Organization type is required and must be one of: SOLO_PRACTITIONER, SMALL_TAX_FIRM, GROWING_PRACTICE, BUSINESS";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidRegistrationOrganizationType, OrganizationType> {

        @Override
        public boolean isValid(OrganizationType value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            return value != OrganizationType.UNKNOWN;
        }
    }
}
