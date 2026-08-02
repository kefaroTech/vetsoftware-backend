package com.vetsoftware.app.module.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteModuleUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
