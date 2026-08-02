package com.vetsoftware.app.tax.application.port.in;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindTaxUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or " + "(hasAuthority('tax.read') and @authz.isMyCompany(#companyId))")
  TaxDto findById(Long id, Long companyId);
}
