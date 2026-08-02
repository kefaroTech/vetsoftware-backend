package com.vetsoftware.app.economicactivity.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteEconomicActivityUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
