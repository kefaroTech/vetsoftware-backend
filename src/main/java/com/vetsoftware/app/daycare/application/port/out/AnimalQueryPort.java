package com.vetsoftware.app.daycare.application.port.out;

import com.vetsoftware.app.daycare.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findById(Long animalId);
}
