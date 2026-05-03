package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationRepository implements HospitalizationRepository {
    private final HospitalizationJpaRepository jpaRepository;
    private final HospitalizationJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaHospitalizationRepository(HospitalizationJpaRepository jpaRepository,
                                        HospitalizationJpaMapper mapper,
                                        AnimalJpaRepository animalJpaRepository,
                                        CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Hospitalization save(Hospitalization hospitalization) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(hospitalization.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(hospitalization.getCompany().id());
        HospitalizationJpaEntity saved = jpaRepository.save(mapper.toJpa(hospitalization, animal, company));
        return mapper.toDomain(saved, hospitalization.getAnimal(), hospitalization.getCompany());
    }

    @Override
    public Optional<Hospitalization> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Hospitalization> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
