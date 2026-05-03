package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindVaccinationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    VaccinationDto findById(Long id);
}
