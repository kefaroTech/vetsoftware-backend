package com.vetsoftware.app.hospitalizationprocedure.application.port.out;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import java.util.Optional;

/**
 * Sin variante ancha a proposito: la unica forma de resolver la hospitalizacion
 * es acotando por empresa. Con un {@code findById(id)} pelado, un hijo de esta
 * empresa podia quedar colgado de la hospitalizacion de otro tenant —la carga
 * propia ya acotada no lo impide, porque el defecto no es apropiarse de la fila
 * sino colgarla de un padre ajeno—, y el resultado es una anotacion clinica
 * escrita por una veterinaria dentro del expediente de un paciente de otra.
 */
public interface HospitalizationQueryPort {
    Optional<HospitalizationRef> findByIdAndCompanyId(Long hospitalizationId, Long companyId);
}
