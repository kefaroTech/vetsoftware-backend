package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import java.util.Optional;

/**
 * La referencia al animal se resuelve <b>siempre</b> acotada por empresa, y la
 * variante ancha no existe a proposito.
 *
 * <p>
 * Con la carga propia ya acotada, un {@code UpdateLaboratoryTestService} no
 * puede apropiarse de la orden ajena; lo que si podia era <b>reapuntar la suya
 * al animal de otro tenant</b> —un laboratorio de mi empresa en la historia
 * clinica de la vecina—, porque resolvia la referencia con
 * {@code findById(animalId)} y ese metodo no filtraba nada. Quitandolo del
 * puerto la fuga no queda «sin uso»: queda imposible de escribir.
 */
public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
