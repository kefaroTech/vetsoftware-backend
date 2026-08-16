package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationsUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes lo abria tambien {@code hospitalization.read}, que es un permiso de
     * empleado, y este listado no filtra por tenant: cualquier empleado listaba
     * tambien las filas de las demas empresas. Lo que un tenant necesita es
     * {@code GET /hospitalizations/by-company}, que si filtra por empresa (BE-29).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<HospitalizationDto> listAll();
}
