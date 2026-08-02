package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
  Optional<AnimalRef> findById(Long animalId);
}
