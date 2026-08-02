package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.command.UpdateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateVaccinationUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('vaccination.update')")
  VaccinationDto execute(UpdateVaccinationCommand command);
}
