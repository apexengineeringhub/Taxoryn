package com.taxoryn.core.domain;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        return SecurityUtils.getCurrentUser()
                .map(SecurityUser::getUsername)
                .or(() -> Optional.of("SYSTEM"));
    }
}
