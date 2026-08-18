package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.port.in.LogoutUseCase;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "auth.logout")
@Service
public class LogoutService implements LogoutUseCase {

    private static final String NOT_A_USER_CONTEXT = "Not an authenticated user context";

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthEmployeeRepository authEmployeeRepository;
    private final AuthSystemUserRepository authSystemUserRepository;

    public LogoutService(RefreshTokenRepository refreshTokenRepository,
            AuthEmployeeRepository authEmployeeRepository,
            AuthSystemUserRepository authSystemUserRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authEmployeeRepository = authEmployeeRepository;
        this.authSystemUserRepository = authSystemUserRepository;
    }

    @Override
    @Transactional
    public void execute() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthContext principal = AuthContext.ofPrincipal(auth == null ? null : auth.getPrincipal());

        switch (principal) {
            case EmployeeContext me -> {
                refreshTokenRepository.revokeAllForSubject(me.employeeId(), "EMPLOYEE");
                // Invalida de inmediato los access tokens vivos (todas las sesiones del
                // empleado).
                authEmployeeRepository.bumpAuthVersion(me.employeeId(), me.companyId());
            }
            case SystemUserContext me -> {
                refreshTokenRepository.revokeAllForSubject(me.systemUserId(), "SYSTEM_USER");
                authSystemUserRepository.bumpAuthVersion(me.systemUserId());
            }
            case SystemContext _ -> throw new AccessDeniedException(NOT_A_USER_CONTEXT);
            case null -> throw new AccessDeniedException(NOT_A_USER_CONTEXT);
        }
    }
}
