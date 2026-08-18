package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import java.util.Optional;

/**
 * Misma razon que en {@link AnimalQueryPort}: colgar la orden propia de la
 * consulta de otro tenant es la misma fuga con otro padre, asi que aqui tampoco
 * hay variante ancha.
 */
public interface ConsultationQueryPort {
    Optional<ConsultationRef> findByIdAndCompanyId(Long consultationId, Long companyId);
}
