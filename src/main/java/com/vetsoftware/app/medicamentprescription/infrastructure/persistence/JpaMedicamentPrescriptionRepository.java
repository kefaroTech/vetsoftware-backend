package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMedicamentPrescriptionRepository implements MedicamentPrescriptionRepository {
    private final MedicamentPrescriptionJpaRepository jpaRepository;
    private final MedicamentPrescriptionJpaMapper mapper;
    private final PrescriptionJpaRepository prescriptionJpaRepository;

    public JpaMedicamentPrescriptionRepository(MedicamentPrescriptionJpaRepository jpaRepository,
                                               MedicamentPrescriptionJpaMapper mapper,
                                               PrescriptionJpaRepository prescriptionJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.prescriptionJpaRepository = prescriptionJpaRepository;
    }

    @Override
    public MedicamentPrescription save(MedicamentPrescription medicament) {
        PrescriptionJpaEntity prescription =
            prescriptionJpaRepository.getReferenceById(medicament.getPrescription().id());
        MedicamentPrescriptionJpaEntity saved = jpaRepository.save(mapper.toJpa(medicament, prescription));
        return mapper.toDomain(saved, medicament.getPrescription());
    }

    @Override
    public Optional<MedicamentPrescription> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MedicamentPrescription> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
