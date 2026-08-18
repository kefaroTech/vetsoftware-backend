package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateVaccinationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('vaccination.update')"
            + " and @authz.isMyCompany(#companyId))")
    VaccinationDto execute(Long id, Long companyId);
}
