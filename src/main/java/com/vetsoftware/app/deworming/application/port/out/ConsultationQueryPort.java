package com.vetsoftware.app.deworming.application.port.out;

import com.vetsoftware.app.deworming.domain.ConsultationRef;
import java.util.Optional;

/**
 * Sin variante ancha a proposito: la unica forma de resolver la consulta es
 * acotando por empresa. Con un {@code findById(id)} pelado, una desparasitacion
 * de esta empresa podia quedar colgada de la consulta de otro tenant.
 */
public interface ConsultationQueryPort {
    Optional<ConsultationRef> findByIdAndCompanyId(Long consultationId, Long companyId);
}
