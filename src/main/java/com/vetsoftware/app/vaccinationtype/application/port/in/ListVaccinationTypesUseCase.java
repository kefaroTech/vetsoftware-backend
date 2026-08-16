package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVaccinationTypesUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes lo abria tambien {@code vaccination.read}, que es un permiso de
     * empleado, y este listado no filtra por tenant: cualquier empleado listaba
     * tambien las filas de las demas empresas. Lo que un tenant necesita es
     * {@code GET /vaccination-types/available}, que si filtra por empresa (BE-29).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<VaccinationTypeDto> listAll();
}
