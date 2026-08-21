package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaEntity;
import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
        PrescriptionJpaEntity prescription = prescriptionJpaRepository
                .getReferenceById(medicament.getPrescription().id());
        MedicamentJpaEntity medicamentCatalog = medicamentJpaRepository
                .getReferenceById(medicament.getMedicamentId());
        MedicamentPrescriptionJpaEntity saved = jpaRepository
                .save(mapper.toJpa(medicament, prescription, medicamentCatalog));
        return mapper.toDomain(saved, medicament.getPrescription(), medicament.getMedicament());
    }

    @Override
    public PageResult<MedicamentPrescription> findAll(int page, int pageSize) {
        // Orden descendente por id: es un listado de administracion donde lo ultimo
        // recetado es lo que interesa primero, y el id da un orden total, que es lo
        // que hace la paginacion determinista.
        Page<MedicamentPrescriptionJpaEntity> result = jpaRepository
                .findAll(Pages.request(page, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        return Pages.result(result, mapper::toDomain);
    }
}
