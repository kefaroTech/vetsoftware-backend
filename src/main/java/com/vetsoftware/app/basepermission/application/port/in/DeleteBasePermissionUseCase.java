package com.vetsoftware.app.basepermission.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteBasePermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
