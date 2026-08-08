package com.vetsoftware.app.animalcolor.application.port.out;

import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import java.util.Optional;

public interface SpecieQueryPort {
    Optional<SpecieRef> findById(Long specieId);
}
