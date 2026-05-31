package com.vetsoftware.app.medicationschedule.application.port.out;

import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import java.util.Optional;

public interface HospitalizationMedicationQueryPort {
    Optional<MedicationOrderParams> findById(Long hospitalizationMedicationId);
}
