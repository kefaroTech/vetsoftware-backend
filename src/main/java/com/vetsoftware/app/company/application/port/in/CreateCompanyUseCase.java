package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateCompanyUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('company.create')")
  CompanyDto execute(CreateCompanyCommand command);
}
