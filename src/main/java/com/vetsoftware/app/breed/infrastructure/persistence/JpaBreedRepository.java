package com.vetsoftware.app.breed.infrastructure.persistence;

import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBreedRepository implements BreedRepository {
    private final BreedJpaRepository jpaRepository;
    private final BreedJpaMapper mapper;
    private final SpecieJpaRepository specieJpaRepository;

    public JpaBreedRepository(BreedJpaRepository jpaRepository,
                              BreedJpaMapper mapper,
                              SpecieJpaRepository specieJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.specieJpaRepository = specieJpaRepository;
    }

    @Override
    public Breed save(Breed breed) {
        SpecieJpaEntity specie = specieJpaRepository.getReferenceById(breed.getSpecie().id());
        BreedJpaEntity saved = jpaRepository.save(mapper.toJpa(breed, specie));
        return mapper.toDomain(saved, breed.getSpecie());
    }

    @Override
    public Optional<Breed> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Breed> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Breed> findBySpecieId(Long specieId) {
        return jpaRepository.findAllBySpecie_Id(specieId).stream().map(mapper::toDomain).toList();
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
