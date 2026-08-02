package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
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

  private final RefreshTokenRepository refreshTokenRepository;
  private final AuthEmployeeRepository authEmployeeRepository;
  private final AuthSystemUserRepository authSystemUserRepository;

  public LogoutService(
      RefreshTokenRepository refreshTokenRepository,
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
    Object principal = auth == null ? null : auth.getPrincipal();

    if (principal instanceof EmployeeContext me) {
      refreshTokenRepository.revokeAllForSubject(me.employeeId(), "EMPLOYEE");
      // Invalida de inmediato los access tokens vivos (todas las sesiones del empleado).
      authEmployeeRepository.bumpAuthVersion(me.employeeId());
    } else if (principal instanceof SystemUserContext me) {
      refreshTokenRepository.revokeAllForSubject(me.systemUserId(), "SYSTEM_USER");
      authSystemUserRepository.bumpAuthVersion(me.systemUserId());
    } else {
      throw new AccessDeniedException("Not an authenticated user context");
    }
  }
}
