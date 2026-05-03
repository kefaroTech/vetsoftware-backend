package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.dto.SystemContext;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SystemAuthRunner {

    private static final Authentication SYSTEM_AUTH = new UsernamePasswordAuthenticationToken(
        SystemContext.INSTANCE,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))
    );

    public <T> T call(Supplier<T> action) {
        Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(SYSTEM_AUTH);
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    public void run(Runnable action) {
        call(() -> { action.run(); return null; });
    }
}
