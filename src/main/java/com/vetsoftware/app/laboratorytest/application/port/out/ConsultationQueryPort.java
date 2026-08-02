package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import java.util.Optional;

public interface ConsultationQueryPort {
  Optional<ConsultationRef> findById(Long consultationId);
}
