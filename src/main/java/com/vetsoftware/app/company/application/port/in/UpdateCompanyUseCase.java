package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCompanyUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('company.update') or hasRole('SYSTEM')")
    CompanyDto execute(UpdateCompanyCommand command);
}
