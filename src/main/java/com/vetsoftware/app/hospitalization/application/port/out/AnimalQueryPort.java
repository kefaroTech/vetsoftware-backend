package com.vetsoftware.app.hospitalization.application.port.out;

import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findById(Long animalId);
}
