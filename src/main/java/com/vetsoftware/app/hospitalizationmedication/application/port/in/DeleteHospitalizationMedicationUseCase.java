package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationMedicationUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.delete')")
  void execute(Long id);
}
