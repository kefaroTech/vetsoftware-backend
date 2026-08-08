package com.vetsoftware.app.animalcolor.application.port.in;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import java.util.List;

@NoAuthorizationRequired(reason = "Catálogo maestro global de solo lectura: no contiene datos de ninguna empresa, así que no hay nada que aislar por tenant.")
public interface ListAnimalColorsUseCase {
    List<AnimalColorDto> listAll();
}
