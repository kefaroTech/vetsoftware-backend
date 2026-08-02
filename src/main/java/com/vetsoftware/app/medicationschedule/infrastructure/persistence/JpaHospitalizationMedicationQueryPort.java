package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaRepository;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaHospitalizationMedicationQueryPort implements HospitalizationMedicationQueryPort {
    private final HospitalizationMedicationJpaRepository medicationJpaRepository;

    public JpaHospitalizationMedicationQueryPort(
            HospitalizationMedicationJpaRepository medicationJpaRepository) {
        this.medicationJpaRepository = medicationJpaRepository;
    }

    @Override
    public Optional<MedicationOrderParams> findById(Long hospitalizationMedicationId) {
        return medicationJpaRepository.findById(hospitalizationMedicationId)
                .map(m -> new MedicationOrderParams(m.getId(), m.getName(),
                        m.getHospitalization().getId(), m.getFrequency(), m.getGuidelineType(),
                        m.getDurationMeasure(), m.getDurationQuantity(), m.getStartDate(),
                        m.getStartTime()));
    }
}
