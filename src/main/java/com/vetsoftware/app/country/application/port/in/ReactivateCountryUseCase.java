package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.country.application.dto.CountryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCountryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('country.update') or hasRole('SYSTEM')")
    CountryDto execute(Long id);
}
