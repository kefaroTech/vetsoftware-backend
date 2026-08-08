package com.vetsoftware.app.prescription.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.prescription.application.dto.PageResult;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPrescriptionRepository implements PrescriptionRepository {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final PrescriptionJpaRepository jpaRepository;
    private final PrescriptionJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaPrescriptionRepository(PrescriptionJpaRepository jpaRepository,
            PrescriptionJpaMapper mapper, AnimalJpaRepository animalJpaRepository,
            ConsultationJpaRepository consultationJpaRepository,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.consultationJpaRepository = consultationJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Prescription save(Prescription prescription) {
        AnimalJpaEntity animal = animalJpaRepository
                .getReferenceById(prescription.getAnimal().id());
        ConsultationJpaEntity consultation = consultationJpaRepository
                .getReferenceById(prescription.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(prescription.getCompany().id());
        PrescriptionJpaEntity saved = jpaRepository
                .save(mapper.toJpa(prescription, animal, consultation, company));
        return mapper.toDomain(saved, prescription.getAnimal(), prescription.getConsultation(),
                prescription.getCompany());
    }

    @Override
    public Optional<Prescription> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Prescription> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Prescription> findAll(int page, int pageSize) {
        int safeSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<PrescriptionJpaEntity> result = jpaRepository.findAll(
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id")));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
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
