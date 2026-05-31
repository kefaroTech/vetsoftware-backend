package com.vetsoftware.app.medicationschedule.application.port.out;

import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import java.util.List;
import java.util.Optional;

public interface MedicationScheduleRepository {
    MedicationSchedule save(MedicationSchedule medicationSchedule);
    Optional<MedicationSchedule> findById(Long id);
    List<MedicationSchedule> findByHospitalizationMedicationId(Long hospitalizationMedicationId);
    void delete(Long id);
    int reactivate(Long id);
}
