package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
  Optional<AnimalRef> findById(Long animalId);
}
