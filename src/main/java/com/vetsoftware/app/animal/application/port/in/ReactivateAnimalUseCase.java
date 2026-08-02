package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateAnimalUseCase {
  @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.update'))")
  AnimalDto execute(Long id, Long companyId);
}
