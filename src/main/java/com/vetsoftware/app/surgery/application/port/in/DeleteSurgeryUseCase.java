package com.vetsoftware.app.surgery.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSurgeryUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('surgery.delete')")
  void execute(Long id);
}
