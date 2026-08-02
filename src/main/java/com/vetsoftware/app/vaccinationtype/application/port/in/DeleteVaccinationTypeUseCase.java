package com.vetsoftware.app.vaccinationtype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteVaccinationTypeUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('vaccination.delete')")
  void execute(Long id);
}
