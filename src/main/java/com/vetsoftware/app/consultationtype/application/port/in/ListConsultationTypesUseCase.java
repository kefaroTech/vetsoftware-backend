package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;
import java.util.List;

@NoAuthorizationRequired(reason = "Catálogo maestro global de solo lectura: no contiene datos de ninguna empresa, así que no hay nada que aislar por tenant.")
public interface ListConsultationTypesUseCase {
    List<ConsultationTypeDto> listAll();
}
