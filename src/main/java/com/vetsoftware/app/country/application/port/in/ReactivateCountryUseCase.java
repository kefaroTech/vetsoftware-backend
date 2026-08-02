package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.country.application.dto.CountryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCountryUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('country.update')")
  CountryDto execute(Long id);
}
