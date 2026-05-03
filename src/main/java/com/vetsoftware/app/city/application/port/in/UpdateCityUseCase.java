package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.command.UpdateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateCityUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    CityDto execute(UpdateCityCommand command);
}
