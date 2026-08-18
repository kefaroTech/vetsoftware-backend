package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.domain.ConsultationRef;
import java.util.Optional;

/**
 * Sin variante ancha a proposito: la unica forma de resolver la consulta es
 * acotando por empresa. Dejar un {@code findById(id)} disponible es dejar la
 * fuga a mano del proximo copy-paste — una receta colgada de la consulta de
 * otro tenant.
 */
public interface ConsultationQueryPort {
    Optional<ConsultationRef> findByIdAndCompanyId(Long consultationId, Long companyId);
}
