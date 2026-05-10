package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.ConsultationRef;
import java.util.Optional;

public interface ConsultationQueryPort {
    Optional<ConsultationRef> findById(Long consultationId);
}
