package com.vetsoftware.app.laboratorytesttype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteLaboratoryTestTypeUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.delete')")
  void execute(Long id);
}
