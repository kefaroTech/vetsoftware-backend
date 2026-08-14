package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDiagnosticImagingTypesUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes lo abria tambien {@code diagnosticimaging.read}, que es un permiso de
     * empleado, y este listado no filtra por tenant: cualquier empleado listaba
     * tambien las filas de las demas empresas. Lo que un tenant necesita es
     * {@code GET /diagnostic-imaging-types/available}, que si filtra por empresa
     * (BE-29).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<DiagnosticImagingTypeDto> listAll();
}
