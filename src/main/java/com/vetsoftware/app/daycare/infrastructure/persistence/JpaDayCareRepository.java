package com.vetsoftware.app.daycare.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCare;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDayCareRepository implements DayCareRepository {
    private final DayCareJpaRepository jpaRepository;
    private final DayCareJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaDayCareRepository(DayCareJpaRepository jpaRepository,
                                DayCareJpaMapper mapper,
                                AnimalJpaRepository animalJpaRepository,
                                CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public DayCare save(DayCare dayCare) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(dayCare.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(dayCare.getCompany().id());
        DayCareJpaEntity saved = jpaRepository.save(mapper.toJpa(dayCare, animal, company));
        return mapper.toDomain(saved, dayCare.getAnimal(), dayCare.getCompany());
    }

    @Override
    public Optional<DayCare> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DayCare> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DayCare> findAllByAnimalId(Long animalId) {
        return jpaRepository.findAllByAnimalId(animalId).stream().map(mapper::toDomain).toList();
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
