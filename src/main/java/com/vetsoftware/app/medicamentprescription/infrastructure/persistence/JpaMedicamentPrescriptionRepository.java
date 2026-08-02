package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaEntity;
import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaRepository;
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
    private final MedicamentJpaRepository medicamentJpaRepository;

    public JpaMedicamentPrescriptionRepository(MedicamentPrescriptionJpaRepository jpaRepository,
                                               MedicamentPrescriptionJpaMapper mapper,
                                               PrescriptionJpaRepository prescriptionJpaRepository,
                                               MedicamentJpaRepository medicamentJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.prescriptionJpaRepository = prescriptionJpaRepository;
        this.medicamentJpaRepository = medicamentJpaRepository;
    }

    @Override
    public MedicamentPrescription save(MedicamentPrescription medicament) {
        PrescriptionJpaEntity prescription =
            prescriptionJpaRepository.getReferenceById(medicament.getPrescription().id());
        MedicamentJpaEntity medicamentCatalog =
            medicamentJpaRepository.getReferenceById(medicament.getMedicamentId());
        MedicamentPrescriptionJpaEntity saved =
            jpaRepository.save(mapper.toJpa(medicament, prescription, medicamentCatalog));
        return mapper.toDomain(saved, medicament.getPrescription(), medicament.getMedicament());
    }

    @Override
    public Optional<MedicamentPrescription> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MedicamentPrescription> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndPrescription_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<MedicamentPrescription> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
