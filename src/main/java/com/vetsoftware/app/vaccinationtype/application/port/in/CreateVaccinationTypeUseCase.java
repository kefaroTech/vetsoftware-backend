package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateVaccinationTypeUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('vaccination.create')")
  VaccinationTypeDto execute(CreateVaccinationTypeCommand command);
}
