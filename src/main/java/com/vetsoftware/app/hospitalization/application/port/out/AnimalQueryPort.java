package com.vetsoftware.app.hospitalization.application.port.out;

import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import java.util.Optional;

/**
 * La referencia al animal se resuelve <b>siempre</b> acotada por empresa, y la
 * variante ancha no existe a proposito.
 *
 * <p>
 * Con la carga propia ya acotada, un {@code UpdateHospitalizationService} no
 * puede apropiarse de la hospitalizacion ajena; lo que si podia era
 * <b>reapuntar la suya al animal de otro tenant</b> —un paciente ingresado en
 * mi empresa colgado del animal de la vecina—, porque resolvia la referencia
 * con {@code findById(animalId)} y ese metodo no filtraba nada. Quitandolo del
 * puerto la fuga no queda «sin uso»: queda imposible de escribir.
 */
public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
