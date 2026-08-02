package com.vetsoftware.app.medicamentprescription.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteMedicamentPrescriptionUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('medicamentPrescription.delete') and @authz.isMyCompany(#companyId))")
  void execute(Long id, Long companyId);
}
