package com.vetsoftware.app.medicamentprescription.application.port.in;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindMedicamentPrescriptionUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('prescription.read') and @authz.isMyCompany(#companyId))")
  MedicamentPrescriptionDto findById(Long id, Long companyId);
}
