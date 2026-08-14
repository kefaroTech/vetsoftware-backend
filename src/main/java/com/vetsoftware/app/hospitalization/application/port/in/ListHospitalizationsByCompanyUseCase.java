package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationsByCompanyUseCase {
    /**
     * Las hospitalizaciones de una empresa. Es lo que necesita el tablero de la
     * sala: {@code listAll()} devuelve las de todas, y hasta BE-29 el tablero lo
     * usaba y filtraba en el navegador, de modo que llegaban al front las estancias
     * de otros clientes.
     */
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('hospitalization.read') and @authz.isMyCompany(#companyId))")
    List<HospitalizationDto> listByCompany(Long companyId);
}
