package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateVaccinationTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('vaccination.update')")
    VaccinationTypeDto execute(UpdateVaccinationTypeCommand command);
}
