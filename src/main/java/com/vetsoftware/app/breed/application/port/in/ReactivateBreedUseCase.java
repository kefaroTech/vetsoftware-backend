package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateBreedUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  BreedDto execute(Long id);
}
