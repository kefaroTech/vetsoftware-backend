package com.vetsoftware.app.medicament.application.port.in;

import com.vetsoftware.app.medicament.application.command.CreateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alta en el catalogo GLOBAL, el que comparten todos los tenants. Puerto
 * dedicado y no una rama de {@link CreateMedicamentUseCase}: el CLAUDE.md
 * prohibe mezclar admin global y employee-scoped en el mismo caso de uso, y
 * aqui la prohibicion tiene consecuencia concreta. El puerto del tenant se abre
 * con {@code hasAuthority('prescription.create')}, que llevan los perfiles
 * clinicos: reutilizarlo dejaria a cualquier veterinario escribiendo el
 * vademecum de la plataforma.
 *
 * <p>
 * {@code hasRole('SYSTEM')} a secas, sin disyuncion de authority, porque no hay
 * empresa que acotar: la fila que escribe este puerto no es de nadie.
 */
public interface CreateGlobalMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    MedicamentDto execute(CreateGlobalMedicamentCommand command);
}
