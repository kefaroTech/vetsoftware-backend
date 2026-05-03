package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCompanyUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('company.read') and @authz.isMyCompany(#id))")
    CompanyDto findById(Long id);
}
