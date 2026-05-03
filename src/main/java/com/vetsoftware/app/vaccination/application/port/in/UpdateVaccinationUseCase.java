package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.command.UpdateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateVaccinationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    VaccinationDto execute(UpdateVaccinationCommand command);
}
