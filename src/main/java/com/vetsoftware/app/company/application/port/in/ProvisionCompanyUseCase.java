package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Provisionamiento de plataforma de una empresa junto con su contrato inicial.
 */
public interface ProvisionCompanyUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyDto execute(CreateCompanyCommand command);
}
