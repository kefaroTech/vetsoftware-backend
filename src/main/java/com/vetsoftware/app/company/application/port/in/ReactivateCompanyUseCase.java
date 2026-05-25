package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCompanyUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('company.update') or hasRole('SYSTEM')")
    CompanyDto execute(Long id);
}
