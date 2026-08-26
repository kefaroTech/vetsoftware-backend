package com.vetsoftware.app.medicament.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pausa (baja logica, {@code @SQLDelete}) un medicamento del catalogo GLOBAL.
 * Ver {@link CreateGlobalMedicamentUseCase} para por que es un puerto aparte.
 *
 * <p>
 * No recibe empresa y aun asi es seguro porque el gate es SYSTEM exacto y el
 * servicio filtra por {@code isGeneral}: un id de fila privada no lo alcanza.
 */
public interface DeleteGlobalMedicamentUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long id);
}
