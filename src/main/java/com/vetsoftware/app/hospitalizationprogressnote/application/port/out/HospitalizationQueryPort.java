package com.vetsoftware.app.hospitalizationprogressnote.application.port.out;

import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import java.util.Optional;

public interface HospitalizationQueryPort {
    Optional<HospitalizationRef> findById(Long hospitalizationId);
}
