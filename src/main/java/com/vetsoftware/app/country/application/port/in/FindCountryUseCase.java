package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.country.application.dto.CountryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCountryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    CountryDto findById(Long id);
}
