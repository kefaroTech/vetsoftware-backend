package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCompanyUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('company.update')")
  CompanyDto execute(Long id);
}
