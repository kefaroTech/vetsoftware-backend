package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.dto.CityDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListCitiesUseCase {
    List<CityDto> listAll();
}
