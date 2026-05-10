package com.vetsoftware.app.hospitalization.application.port.out;

import com.vetsoftware.app.hospitalization.domain.ConsultationRef;
import java.util.Optional;

public interface ConsultationQueryPort {
    Optional<ConsultationRef> findById(Long consultationId);
}
