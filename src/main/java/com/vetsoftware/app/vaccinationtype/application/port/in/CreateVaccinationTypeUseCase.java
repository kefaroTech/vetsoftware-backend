package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateVaccinationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccination.create') or hasRole('SYSTEM')")
    VaccinationTypeDto execute(CreateVaccinationTypeCommand command);
}
