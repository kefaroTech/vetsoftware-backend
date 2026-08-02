package com.vetsoftware.app.country.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCountryUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
