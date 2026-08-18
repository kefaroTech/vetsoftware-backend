package com.vetsoftware.app.diagnosticimaging.application.port.out;

import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import java.util.Optional;

/**
 * La referencia al animal se resuelve <b>siempre</b> acotada por empresa, y la
 * variante ancha no existe a proposito.
 *
 * <p>
 * Con la carga propia ya acotada, un {@code UpdateDiagnosticImagingService} no
 * puede apropiarse del estudio ajeno; lo que si podia era <b>reapuntar el suyo
 * al animal de otro tenant</b> —una imagen con su diagnostico en la historia
 * clinica de la vecina—, porque resolvia la referencia con
 * {@code findById(animalId)} y ese metodo no filtraba nada. Quitandolo del
 * puerto la fuga no queda «sin uso»: queda imposible de escribir.
 */
public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
