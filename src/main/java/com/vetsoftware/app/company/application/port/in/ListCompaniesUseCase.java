package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListCompaniesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('company.read')")
    List<CompanyDto> listAll();
}
