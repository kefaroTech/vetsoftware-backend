package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.domain.ConsultationRef;
import java.util.Optional;

public interface ConsultationQueryPort {
    Optional<ConsultationRef> findById(Long consultationId);
}
