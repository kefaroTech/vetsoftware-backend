package com.vetsoftware.app.company.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCompanyUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('company.delete')")
  void execute(Long id);
}
