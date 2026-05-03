package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListCompaniesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('company.read') or hasRole('SYSTEM')")
    List<CompanyDto> listAll();
}
