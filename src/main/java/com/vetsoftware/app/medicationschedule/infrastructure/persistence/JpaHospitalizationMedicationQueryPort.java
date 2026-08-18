package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaEntity;
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
        return medicationJpaRepository.findById(hospitalizationMedicationId).map(this::toParams);
    }

    /**
     * {@code findByIdAndHospitalization_Company_Id} es el JOIN que sube de la orden
     * a la hospitalizacion y de ahi a la empresa; el {@code @EntityGraph} del
     * repositorio ya trae la hospitalizacion, asi que sigue siendo una sola query.
     */
    @Override
    public Optional<MedicationOrderParams> findByIdAndCompanyId(Long hospitalizationMedicationId,
            Long companyId) {
        return medicationJpaRepository
                .findByIdAndHospitalization_Company_Id(hospitalizationMedicationId, companyId)
                .map(this::toParams);
    }

    private MedicationOrderParams toParams(HospitalizationMedicationJpaEntity m) {
        return new MedicationOrderParams(m.getId(), m.getName(), m.getHospitalization().getId(),
                m.getFrequency(), m.getGuidelineType(), m.getDurationMeasure(),
                m.getDurationQuantity(), m.getStartDate(), m.getStartTime());
    }
}
