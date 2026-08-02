package com.vetsoftware.app.problem.application.port.out;

import com.vetsoftware.app.problem.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
  Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
