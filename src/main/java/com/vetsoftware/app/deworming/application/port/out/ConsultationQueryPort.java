package com.vetsoftware.app.deworming.application.port.out;

import com.vetsoftware.app.deworming.domain.ConsultationRef;
import java.util.Optional;

public interface ConsultationQueryPort {
    Optional<ConsultationRef> findById(Long consultationId);
}
