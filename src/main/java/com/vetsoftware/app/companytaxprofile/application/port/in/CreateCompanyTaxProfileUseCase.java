package com.vetsoftware.app.companytaxprofile.application.port.in;

import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateCompanyTaxProfileUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('companyTaxProfile.manage') and @authz.isMyCompany(#command.companyId))")
    CompanyTaxProfileDto execute(CreateCompanyTaxProfileCommand command);
}
