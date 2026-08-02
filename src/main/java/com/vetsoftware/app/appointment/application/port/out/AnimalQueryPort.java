package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
  Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
