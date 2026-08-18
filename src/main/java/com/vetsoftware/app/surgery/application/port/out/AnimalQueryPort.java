package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.AnimalRef;
import java.util.Optional;

/**
 * La referencia al animal se resuelve <b>siempre</b> acotada por empresa, y la
 * variante ancha no existe a proposito.
 *
 * <p>
 * Con la carga propia ya acotada, un {@code UpdateSurgeryService} no puede
 * apropiarse de la cirugia ajena; lo que si podia era <b>reapuntar la suya al
 * animal de otro tenant</b> —una cirugia de mi empresa en la historia clinica
 * de la vecina—, porque resolvia la referencia con {@code findById(animalId)} y
 * ese metodo no filtraba nada. Quitandolo del puerto la fuga no queda «sin
 * usar»: queda imposible de escribir.
 */
public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
