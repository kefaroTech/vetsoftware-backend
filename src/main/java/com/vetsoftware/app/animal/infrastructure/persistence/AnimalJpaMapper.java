package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AnimalJpaMapper {

    public AnimalJpaEntity toJpa(Animal animal,
                                  SpecieJpaEntity specie,
                                  BreedJpaEntity breed,
                                  OwnerJpaEntity owner,
                                  CompanyJpaEntity company) {
        AnimalJpaEntity entity = new AnimalJpaEntity();
        entity.setId(animal.getId());
        entity.setName(animal.getName());
        entity.setCode(animal.getCode());
        entity.setSpecie(specie);
        entity.setBreed(breed);
        entity.setOwner(owner);
        entity.setGender(animal.getGender());
        entity.setWeightType(animal.getWeightType());
        entity.setAnimalType(animal.getAnimalType());
        entity.setReproductiveState(animal.getReproductiveState());
        entity.setColor(animal.getColor());
        entity.setBod(animal.getBod());
        entity.setWeight(animal.getWeight());
        entity.setSize(animal.getSize());
        entity.setDeceased(animal.isDeceased());
        entity.setDeceasedDate(animal.getDeceasedDate());
        entity.setCompany(company);
        entity.setCreatedDate(animal.getCreatedDate());
        return entity;
    }

    public Animal toDomain(AnimalJpaEntity entity) {
        SpecieJpaEntity s = entity.getSpecie();
        BreedJpaEntity b = entity.getBreed();
        OwnerJpaEntity o = entity.getOwner();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new SpecieRef(s.getId(), s.getName()),
            new BreedRef(b.getId(), b.getName()),
            new OwnerRef(o.getId(), o.getName(), o.getDocument()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Animal toDomain(AnimalJpaEntity entity, SpecieRef specieRef, BreedRef breedRef,
                           OwnerRef ownerRef, CompanyRef companyRef) {
        return new Animal(
            entity.getId(), entity.getName(), entity.getCode(),
            specieRef, breedRef, ownerRef,
            entity.getGender(), entity.getWeightType(), entity.getAnimalType(),
            entity.getReproductiveState(), entity.getColor(), entity.getBod(),
            entity.getWeight(), entity.getSize(), entity.isDeceased(), entity.getDeceasedDate(),
            companyRef, entity.getCreatedDate()
        );
    }
}
