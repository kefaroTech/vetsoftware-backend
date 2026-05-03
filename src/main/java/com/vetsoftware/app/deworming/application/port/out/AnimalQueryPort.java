package com.vetsoftware.app.deworming.application.port.out;

import com.vetsoftware.app.deworming.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findById(Long animalId);
}
