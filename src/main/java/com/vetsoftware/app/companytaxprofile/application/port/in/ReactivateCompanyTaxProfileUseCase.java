package com.vetsoftware.app.companytaxprofile.application.port.in;

import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCompanyTaxProfileUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('electronicbilling.update') and"
          + " @authz.isMyCompany(#companyId))")
  CompanyTaxProfileDto execute(Long companyId);
}
