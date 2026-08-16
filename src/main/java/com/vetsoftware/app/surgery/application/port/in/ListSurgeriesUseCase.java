package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSurgeriesUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes lo abria tambien {@code surgery.read}, que es un permiso de empleado, y
     * este listado no filtra por tenant: cualquier empleado listaba tambien las
     * filas de las demas empresas. Lo que un tenant necesita es
     * {@code GET /surgeries/by-animal/{animalId}}, que si filtra por empresa
     * (BE-29).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<SurgeryDto> listAll();
}
