package com.taxoryn.core.security.validation;

import com.taxoryn.core.security.PasswordSecurityUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * Jakarta constraint validator backing {@link StrongPassword}.
 * Delegates directly to canonical {@link PasswordSecurityUtils#isStrongProductionPassword(String)}.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private boolean optional;

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return optional;
        }
        return PasswordSecurityUtils.isStrongProductionPassword(value.trim());
    }
}
