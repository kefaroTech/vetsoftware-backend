package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.country.application.command.UpdateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCountryUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    CountryDto execute(UpdateCountryCommand command);
}
