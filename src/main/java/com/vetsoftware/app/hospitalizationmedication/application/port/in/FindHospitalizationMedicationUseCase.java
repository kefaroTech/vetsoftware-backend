package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationMedicationUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('hospitalization.read') and"
          + " @authz.isMyCompany(#companyId))")
  HospitalizationMedicationDto findById(Long id, Long companyId);
}
