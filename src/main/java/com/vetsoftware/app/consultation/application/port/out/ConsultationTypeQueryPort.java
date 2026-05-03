package com.vetsoftware.app.consultation.application.port.out;

import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import java.util.Optional;

public interface ConsultationTypeQueryPort {
    Optional<ConsultationTypeRef> findById(Long consultationTypeId);
}
