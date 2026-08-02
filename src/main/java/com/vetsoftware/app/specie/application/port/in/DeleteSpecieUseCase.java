package com.vetsoftware.app.specie.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSpecieUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
