package com.vetsoftware.app.breed.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteBreedUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  void execute(Long id);
}
