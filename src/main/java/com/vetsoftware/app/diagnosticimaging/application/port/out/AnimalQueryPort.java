package com.vetsoftware.app.diagnosticimaging.application.port.out;

import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
  Optional<AnimalRef> findById(Long animalId);
}
