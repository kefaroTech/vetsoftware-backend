package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import java.util.List;

@NoAuthorizationRequired(reason = "Catálogo maestro global de solo lectura: no contiene datos de ninguna empresa, así que no hay nada que aislar por tenant.")
public interface ListSpeciesUseCase {
    List<SpecieDto> listAll();
}
