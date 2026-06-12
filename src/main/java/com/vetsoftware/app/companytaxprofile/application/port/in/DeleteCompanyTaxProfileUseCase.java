package com.vetsoftware.app.companytaxprofile.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteCompanyTaxProfileUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('companyTaxProfile.manage') and @authz.isMyCompany(#companyId))")
    void execute(Long companyId);
}
