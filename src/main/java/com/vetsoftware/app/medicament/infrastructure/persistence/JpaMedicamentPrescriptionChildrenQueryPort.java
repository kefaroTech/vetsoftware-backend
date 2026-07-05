package com.vetsoftware.app.medicament.infrastructure.persistence;

import com.vetsoftware.app.medicament.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaMedicamentPrescriptionChildrenQueryPort implements MedicamentPrescriptionChildrenQueryPort {
    private final MedicamentPrescriptionJpaRepository jpaRepository;

    public JpaMedicamentPrescriptionChildrenQueryPort(MedicamentPrescriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByMedicamentId(Long medicamentId) {
        return jpaRepository.existsByMedicament_Id(medicamentId);
    }
}
