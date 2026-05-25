package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateVaccinationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccinationtype.update') or hasRole('SYSTEM')")
    VaccinationTypeDto execute(Long id);
}
