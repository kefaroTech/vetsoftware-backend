package com.vetsoftware.app.companytaxprofile.application.port.in;

import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateCompanyTaxProfileUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('electronicbilling.create') and"
            + " @authz.isMyCompany(#command.companyId))")
    CompanyTaxProfileDto execute(CreateCompanyTaxProfileCommand command);
}
