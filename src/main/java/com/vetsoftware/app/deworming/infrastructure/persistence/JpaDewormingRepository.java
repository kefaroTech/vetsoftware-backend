package com.vetsoftware.app.deworming.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.Deworming;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDewormingRepository implements DewormingRepository {
    private final DewormingJpaRepository jpaRepository;
    private final DewormingJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaDewormingRepository(DewormingJpaRepository jpaRepository,
                                  DewormingJpaMapper mapper,
                                  AnimalJpaRepository animalJpaRepository,
                                  CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Deworming save(Deworming deworming) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(deworming.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(deworming.getCompany().id());
        DewormingJpaEntity saved = jpaRepository.save(mapper.toJpa(deworming, animal, company));
        return mapper.toDomain(saved, deworming.getAnimal(), deworming.getCompany());
    }

    @Override
    public Optional<Deworming> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Deworming> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
