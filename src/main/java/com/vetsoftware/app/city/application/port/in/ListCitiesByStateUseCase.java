package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import java.util.List;

@NoAuthorizationRequired(reason = "Catálogo maestro global de solo lectura: no contiene datos de ninguna empresa, así que no hay nada que aislar por tenant.")
public interface ListCitiesByStateUseCase {
    List<CityDto> listByState(Long stateId);
}
