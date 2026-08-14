package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSurgeryTypesUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes lo abria tambien {@code surgery.read}, que es un permiso de empleado, y
     * este listado no filtra por tenant: cualquier empleado listaba tambien las
     * filas de las demas empresas. Lo que un tenant necesita es
     * {@code GET /surgery-types/available}, que si filtra por empresa (BE-29).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<SurgeryTypeDto> listAll();
}
