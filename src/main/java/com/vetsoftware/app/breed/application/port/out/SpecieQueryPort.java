package com.vetsoftware.app.breed.application.port.out;

import com.vetsoftware.app.breed.domain.SpecieRef;
import java.util.Optional;

public interface SpecieQueryPort {
  Optional<SpecieRef> findById(Long specieId);
}
