package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Los medicamentos GLOBALES pausados, que el {@code @SQLRestriction} esconde
 * del catalogo activo. Es la unica pantalla desde la que se puede reactivar uno
 * y por eso existe: sin ella un global pausado es invisible y, en la practica,
 * irrecuperable.
 */
public interface ListDisabledGlobalMedicamentsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<MedicamentDto> listDisabled();
}
