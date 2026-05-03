package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.dto.CityDto;
import java.util.List;

public interface ListCitiesByStateUseCase {
    List<CityDto> listByState(Long stateId);
}
