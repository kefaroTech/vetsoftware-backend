package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSpecieUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  SpecieDto execute(Long id);
}
