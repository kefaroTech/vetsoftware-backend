package com.vetsoftware.app.diagnosticimaging.application.port.out;

import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import java.util.Optional;

public interface ConsultationQueryPort {
    Optional<ConsultationRef> findById(Long consultationId);
}
