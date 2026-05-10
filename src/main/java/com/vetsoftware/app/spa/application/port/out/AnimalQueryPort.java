package com.vetsoftware.app.spa.application.port.out;

import com.vetsoftware.app.spa.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findById(Long animalId);
}
