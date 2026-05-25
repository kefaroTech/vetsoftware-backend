package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateVaccinationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccination.update') or hasRole('SYSTEM')")
    VaccinationDto execute(Long id);
}
