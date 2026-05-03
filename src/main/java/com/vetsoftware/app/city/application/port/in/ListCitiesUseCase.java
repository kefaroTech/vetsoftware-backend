package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.dto.CityDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListCitiesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<CityDto> listAll();
}
