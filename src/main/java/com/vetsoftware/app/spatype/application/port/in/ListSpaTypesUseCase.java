package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import java.util.List;

@NoAuthorizationRequired(reason = "Catálogo maestro global de solo lectura: no contiene datos de ninguna empresa, así que no hay nada que aislar por tenant.")
public interface ListSpaTypesUseCase {
    List<SpaTypeDto> listAll();
}
