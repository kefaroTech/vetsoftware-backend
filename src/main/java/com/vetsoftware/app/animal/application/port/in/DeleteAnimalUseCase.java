package com.vetsoftware.app.animal.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteAnimalUseCase {
  @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.delete'))")
  void execute(Long id, Long companyId);
}
