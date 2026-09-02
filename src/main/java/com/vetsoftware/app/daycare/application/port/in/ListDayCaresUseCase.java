package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDayCaresUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * No filtra por tenant, asi que abrirlo tambien a {@code dayCare.read} —un
     * permiso de empleado— dejaria a cualquier empleado listando las filas de las
     * demas empresas. Lo que un tenant necesita es
     * {@code GET /day-cares/by-animal/{animalId}}, que si filtra por empresa.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<DayCareDto> listAll();
}
