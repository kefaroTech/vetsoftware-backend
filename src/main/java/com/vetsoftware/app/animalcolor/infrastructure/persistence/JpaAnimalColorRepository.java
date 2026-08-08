package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAnimalColorRepository implements AnimalColorRepository {
    private final AnimalColorJpaRepository jpaRepository;
    private final AnimalColorJpaMapper mapper;
    private final SpecieJpaRepository specieJpaRepository;

    public JpaAnimalColorRepository(AnimalColorJpaRepository jpaRepository,
            AnimalColorJpaMapper mapper, SpecieJpaRepository specieJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.specieJpaRepository = specieJpaRepository;
    }

    @Override
    public AnimalColor save(AnimalColor color) {
        SpecieJpaEntity specie = specieJpaRepository.getReferenceById(color.getSpecie().id());
        AnimalColorJpaEntity saved = jpaRepository.save(mapper.toJpa(color, specie));
        return mapper.toDomain(saved, color.getSpecie());
    }

    @Override
    public Optional<AnimalColor> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<AnimalColor> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AnimalColor> findBySpecieId(Long specieId) {
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
