package com.vetsoftware.app.vaccination.application.port.out;

import com.vetsoftware.app.vaccination.domain.ConsultationRef;
import java.util.Optional;

public interface ConsultationQueryPort {
    Optional<ConsultationRef> findById(Long consultationId);
}
