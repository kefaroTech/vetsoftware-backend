package com.vetsoftware.app.vaccination.application.port.in;

import com.vetsoftware.app.vaccination.application.command.CreateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateVaccinationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('vaccination.create') and @authz.isMyCompany(#command.companyId))")
    VaccinationDto execute(CreateVaccinationCommand command);
}
