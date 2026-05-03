package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCompanyUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('company.read') or hasRole('SYSTEM')")
    CompanyDto findById(Long id);
}
