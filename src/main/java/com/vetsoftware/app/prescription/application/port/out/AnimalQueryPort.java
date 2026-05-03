package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findById(Long animalId);
}
