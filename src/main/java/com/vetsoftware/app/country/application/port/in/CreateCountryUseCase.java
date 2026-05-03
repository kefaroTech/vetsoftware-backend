package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.country.application.command.CreateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateCountryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    CountryDto execute(CreateCountryCommand command);
}
