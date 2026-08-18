package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateVaccinationTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('vaccination.update')"
            + " and @authz.isMyCompany(#companyId))")
    VaccinationTypeDto execute(Long id, Long companyId);
}
