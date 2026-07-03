package com.vetsoftware.app.animalalert.application.port.out;

import com.vetsoftware.app.animalalert.domain.AnimalRef;
import java.util.Optional;

public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
