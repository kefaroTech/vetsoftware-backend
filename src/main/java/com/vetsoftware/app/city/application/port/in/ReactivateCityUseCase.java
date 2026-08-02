package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.dto.CityDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateCityUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('city.update')")
  CityDto execute(Long id);
}
