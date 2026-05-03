package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.BreedRef;
import java.util.Optional;

public interface BreedQueryPort {
    Optional<BreedRef> findById(Long breedId);
}
