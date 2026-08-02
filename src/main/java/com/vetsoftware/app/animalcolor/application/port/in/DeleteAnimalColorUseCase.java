package com.vetsoftware.app.animalcolor.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteAnimalColorUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
