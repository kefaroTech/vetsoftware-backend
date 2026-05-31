package com.vetsoftware.app.medicationschedule.application.port.out;

import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import java.util.List;
import java.util.Optional;

public interface MedicationScheduleRepository {
    MedicationSchedule save(MedicationSchedule medicationSchedule);
    Optional<MedicationSchedule> findById(Long id);
    List<MedicationSchedule> findByHospitalizationMedicationId(Long hospitalizationMedicationId);
    List<MedicationSchedule> findByHospitalizationId(Long hospitalizationId);
    void delete(Long id);
    void disableByHospitalizationMedicationId(Long hospitalizationMedicationId);
    /** Deshabilita solo las tomas NO aplicadas (conserva el histórico de las aplicadas). */
    void disablePendingByHospitalizationMedicationId(Long hospitalizationMedicationId);
    int reactivate(Long id);
}
