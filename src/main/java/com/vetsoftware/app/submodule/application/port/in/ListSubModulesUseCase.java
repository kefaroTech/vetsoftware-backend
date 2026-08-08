package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import java.util.List;

@NoAuthorizationRequired(reason = "Catálogo maestro global de solo lectura: no contiene datos de ninguna empresa, así que no hay nada que aislar por tenant.")
public interface ListSubModulesUseCase {
    List<SubModuleDto> listAll();
}
