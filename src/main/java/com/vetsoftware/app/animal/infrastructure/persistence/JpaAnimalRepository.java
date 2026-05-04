package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaRepository;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAnimalRepository implements AnimalRepository {
    private final AnimalJpaRepository jpaRepository;
    private final AnimalJpaMapper mapper;
    private final SpecieJpaRepository specieJpaRepository;
    private final BreedJpaRepository breedJpaRepository;
    private final OwnerJpaRepository ownerJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final AnimalColorJpaRepository animalColorJpaRepository;

    public JpaAnimalRepository(AnimalJpaRepository jpaRepository,
                               AnimalJpaMapper mapper,
                               SpecieJpaRepository specieJpaRepository,
                               BreedJpaRepository breedJpaRepository,
                               OwnerJpaRepository ownerJpaRepository,
                               CompanyJpaRepository companyJpaRepository,
                               AnimalColorJpaRepository animalColorJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.specieJpaRepository = specieJpaRepository;
        this.breedJpaRepository = breedJpaRepository;
        this.ownerJpaRepository = ownerJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.animalColorJpaRepository = animalColorJpaRepository;
    }

    @Override
    public Animal save(Animal animal) {
        SpecieJpaEntity specie = specieJpaRepository.getReferenceById(animal.getSpecie().id());
        BreedJpaEntity breed = breedJpaRepository.getReferenceById(animal.getBreed().id());
        OwnerJpaEntity owner = ownerJpaRepository.getReferenceById(animal.getOwner().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(animal.getCompany().id());
        AnimalColorJpaEntity color = animalColorJpaRepository.getReferenceById(animal.getColor().id());
        AnimalJpaEntity saved = jpaRepository.save(mapper.toJpa(animal, specie, breed, owner, company, color));
        return mapper.toDomain(saved, animal.getSpecie(), animal.getBreed(),
                                animal.getOwner(), animal.getCompany(), animal.getColor());
    }

    @Override
    public Optional<Animal> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Animal> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Animal> findByOwnerIdAndCompanyId(Long ownerId, Long companyId) {
        return jpaRepository.findAllByOwner_IdAndCompany_Id(ownerId, companyId)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
