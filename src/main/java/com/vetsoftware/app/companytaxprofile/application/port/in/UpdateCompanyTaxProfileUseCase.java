package com.vetsoftware.app.companytaxprofile.application.port.in;

import com.vetsoftware.app.companytaxprofile.application.command.UpdateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCompanyTaxProfileUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('companyTaxProfile.manage') and @authz.isMyCompany(#command.companyId))")
    CompanyTaxProfileDto execute(UpdateCompanyTaxProfileCommand command);
}
