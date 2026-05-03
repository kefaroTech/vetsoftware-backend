package com.vetsoftware.app.consultation.application.port.out;

import com.vetsoftware.app.consultation.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findById(Long animalId);
}
