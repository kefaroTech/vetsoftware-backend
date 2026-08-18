package com.vetsoftware.app.deworming.application.port.out;

import com.vetsoftware.app.deworming.domain.AnimalRef;
import java.util.Optional;

/**
 * Sin variante ancha a proposito: la unica forma de resolver el animal es
 * acotando por empresa. Con un {@code findById(id)} pelado, una desparasitacion
 * de esta empresa podia quedar colgada del animal de otro tenant —la carga
 * propia ya acotada no lo impide, porque el defecto no es apropiarse de la fila
 * sino reapuntarla a un padre ajeno.
 */
public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
