package com.vetsoftware.app.hospitalizationprocedure.application.port.out;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import java.util.Optional;

public interface HospitalizationQueryPort {
    Optional<HospitalizationRef> findById(Long hospitalizationId);
}
