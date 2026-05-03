package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateVaccinationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    VaccinationTypeDto execute(UpdateVaccinationTypeCommand command);
}
