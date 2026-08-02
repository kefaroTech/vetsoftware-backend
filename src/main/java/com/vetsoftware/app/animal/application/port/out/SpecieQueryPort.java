package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.SpecieRef;
import java.util.Optional;

public interface SpecieQueryPort {
  Optional<SpecieRef> findById(Long specieId);
}
