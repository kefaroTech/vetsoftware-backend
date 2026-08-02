package com.vetsoftware.app.companytaxprofile.application.port.in;

import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCompanyTaxProfileUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('electronicbilling.read') and @authz.isMyCompany(#companyId))")
    CompanyTaxProfileDto findByCompanyId(Long companyId);
}
