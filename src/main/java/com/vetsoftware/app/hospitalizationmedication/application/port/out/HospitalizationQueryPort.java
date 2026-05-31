package com.vetsoftware.app.hospitalizationmedication.application.port.out;

import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationRef;
import java.util.Optional;

public interface HospitalizationQueryPort {
    Optional<HospitalizationRef> findById(Long hospitalizationId);
}
