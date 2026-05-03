package com.vetsoftware.app.prescription.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.Prescription;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPrescriptionRepository implements PrescriptionRepository {
    private final PrescriptionJpaRepository jpaRepository;
    private final PrescriptionJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaPrescriptionRepository(PrescriptionJpaRepository jpaRepository,
                                     PrescriptionJpaMapper mapper,
                                     AnimalJpaRepository animalJpaRepository,
                                     CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Prescription save(Prescription prescription) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(prescription.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(prescription.getCompany().id());
        PrescriptionJpaEntity saved = jpaRepository.save(mapper.toJpa(prescription, animal, company));
        return mapper.toDomain(saved, prescription.getAnimal(), prescription.getCompany());
    }

    @Override
    public Optional<Prescription> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Prescription> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
