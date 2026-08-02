package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.dto.CityDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCityUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  CityDto findById(Long id);
}
