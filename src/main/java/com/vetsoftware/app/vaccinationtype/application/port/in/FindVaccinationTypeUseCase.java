package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindVaccinationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccination.read') or hasRole('SYSTEM')")
    VaccinationTypeDto findById(Long id);
}
