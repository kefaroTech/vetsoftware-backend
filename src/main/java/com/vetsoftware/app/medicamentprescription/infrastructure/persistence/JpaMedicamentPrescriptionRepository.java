package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaEntity;
import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaRepository;
import com.vetsoftware.app.medicamentprescription.application.dto.PageResult;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMedicamentPrescriptionRepository implements MedicamentPrescriptionRepository {
    private static final int LIST_DEFAULT_PAGE_SIZE = 20;
    private static final int LIST_MAX_PAGE_SIZE = 200;

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
    public Optional<MedicamentPrescription> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MedicamentPrescription> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndPrescription_Company_Id(id, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<MedicamentPrescription> findAll(int page, int pageSize) {
        Page<MedicamentPrescriptionJpaEntity> result = jpaRepository
                .findAll(listPageRequest(page, pageSize));
        List<MedicamentPrescription> content = result.getContent().stream().map(mapper::toDomain)
                .toList();
        return new PageResult<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente y acota el tamano; sin tope se vuelve a
     * pedir la tabla entera por query param. Orden descendente por id: es un
     * listado de administracion donde lo ultimo recetado es lo que interesa
     * primero, y el id da un orden total, que es lo que hace la paginacion
     * determinista.
     */
    private static PageRequest listPageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0
                ? LIST_DEFAULT_PAGE_SIZE
                : Math.min(pageSize, LIST_MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
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
