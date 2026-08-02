package com.vetsoftware.app.medicamentprescription.application.port.out;

import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import java.util.Optional;

public interface MedicamentQueryPort {
    Optional<MedicamentRef> findById(Long medicamentId);

    Optional<MedicamentRef> findAvailableById(Long medicamentId, Long companyId);
}
