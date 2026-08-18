package com.vetsoftware.app.vaccination.application.port.out;

import com.vetsoftware.app.vaccination.domain.AnimalRef;
import java.util.Optional;

/**
 * La referencia al animal se resuelve <b>siempre</b> acotada por empresa, y la
 * variante ancha no existe a proposito.
 *
 * <p>
 * Con la carga propia ya acotada, un {@code UpdateVaccinationService} no puede
 * apropiarse de la vacuna ajena; lo que si podia era <b>reapuntar la suya al
 * animal de otro tenant</b> —un carne de vacunacion de la vecina con una dosis
 * puesta por mi empresa—, porque resolvia la referencia con
 * {@code findById(animalId)} y ese metodo no filtraba nada. Quitandolo del
 * puerto la fuga no queda «sin uso»: queda imposible de escribir.
 */
public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
