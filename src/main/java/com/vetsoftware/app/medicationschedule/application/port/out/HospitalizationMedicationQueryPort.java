package com.vetsoftware.app.medicationschedule.application.port.out;

import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import java.util.Optional;

public interface HospitalizationMedicationQueryPort {
    Optional<MedicationOrderParams> findById(Long hospitalizationMedicationId);

    /**
     * La toma no tiene empresa propia: su unico vinculo con un tenant es la orden
     * de medicacion, y la empresa cuelga de la hospitalizacion padre de esa orden.
     * Acotar por {@code hospitalization_medication_id} NO prueba nada —es una FK
     * ajena, el paciente es de alguien—, asi que este es el finder que sube hasta
     * la empresa y el unico que autoriza escribir sobre una toma.
     */
    Optional<MedicationOrderParams> findByIdAndCompanyId(Long hospitalizationMedicationId,
            Long companyId);
}
