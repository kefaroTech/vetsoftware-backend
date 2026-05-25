package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.dto.CityDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCityUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('city.update') or hasRole('SYSTEM')")
    CityDto execute(Long id);
}
