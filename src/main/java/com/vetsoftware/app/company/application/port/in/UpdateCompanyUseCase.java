package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCompanyUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('company.update')")
    CompanyDto execute(UpdateCompanyCommand command);
}
