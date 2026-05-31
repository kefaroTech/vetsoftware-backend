package com.vetsoftware.app.hospitalizationobservation.application.port.out;

import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import java.util.Optional;

public interface HospitalizationQueryPort {
    Optional<HospitalizationRef> findById(Long hospitalizationId);
}
