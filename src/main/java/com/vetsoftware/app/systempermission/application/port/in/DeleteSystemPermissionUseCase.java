package com.vetsoftware.app.systempermission.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSystemPermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
