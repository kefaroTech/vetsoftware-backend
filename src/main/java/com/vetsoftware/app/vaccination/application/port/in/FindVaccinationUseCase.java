package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindVaccinationUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('vaccination.read') and @authz.isMyCompany(#companyId))")
  VaccinationDto findById(Long id, Long companyId);
}
