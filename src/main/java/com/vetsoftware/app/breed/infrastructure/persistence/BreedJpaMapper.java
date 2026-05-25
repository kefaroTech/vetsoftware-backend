package com.vetsoftware.app.breed.infrastructure.persistence;

import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.SpecieRef;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BreedJpaMapper {

    public BreedJpaEntity toJpa(Breed breed, SpecieJpaEntity specie) {
        BreedJpaEntity entity = new BreedJpaEntity();
        entity.setId(breed.getId());
        entity.setName(breed.getName());
        entity.setSpecie(specie);
        entity.setCreatedDate(breed.getCreatedDate());
        entity.setEnabled(breed.isEnabled());
        return entity;
    }

    public Breed toDomain(BreedJpaEntity entity) {
        SpecieJpaEntity s = entity.getSpecie();
        return toDomain(entity, new SpecieRef(s.getId(), s.getName()));
    }

    public Breed toDomain(BreedJpaEntity entity, SpecieRef ref) {
        return new Breed(entity.getId(), entity.getName(), ref, entity.getCreatedDate(), entity.isEnabled());
    }
}
