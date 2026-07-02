package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository.LatestWeightProjection;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final WeightRecordJpaRepository weightRecordJpaRepository;

    public JpaAnimalRepository(AnimalJpaRepository jpaRepository,
                               AnimalJpaMapper mapper,
                               SpecieJpaRepository specieJpaRepository,
                               BreedJpaRepository breedJpaRepository,
                               OwnerJpaRepository ownerJpaRepository,
                               CompanyJpaRepository companyJpaRepository,
                               AnimalColorJpaRepository animalColorJpaRepository,
                               WeightRecordJpaRepository weightRecordJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.specieJpaRepository = specieJpaRepository;
        this.breedJpaRepository = breedJpaRepository;
        this.ownerJpaRepository = ownerJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.animalColorJpaRepository = animalColorJpaRepository;
        this.weightRecordJpaRepository = weightRecordJpaRepository;
    }

    @Override
    public Animal save(Animal animal) {
        SpecieJpaEntity specie = specieJpaRepository.getReferenceById(animal.getSpecie().id());
        BreedJpaEntity breed = breedJpaRepository.getReferenceById(animal.getBreed().id());
        OwnerJpaEntity owner = ownerJpaRepository.getReferenceById(animal.getOwner().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(animal.getCompany().id());
        AnimalColorJpaEntity color = animalColorJpaRepository.getReferenceById(animal.getColor().id());
        AnimalJpaEntity saved = jpaRepository.save(mapper.toJpa(animal, specie, breed, owner, company, color));
        Animal domain = mapper.toDomain(saved, animal.getSpecie(), animal.getBreed(),
                                animal.getOwner(), animal.getCompany(), animal.getColor());
        return enrich(domain, animal.getCompany().id());
    }

    @Override
    public Optional<Animal> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain).map(a -> enrich(a, a.getCompany().id()));
    }

    @Override
    public Optional<Animal> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain)
            .map(a -> enrich(a, companyId));
    }

    @Override
    public List<Animal> findAll() {
        return enrichAll(jpaRepository.findAll().stream().map(mapper::toDomain).toList());
    }

    @Override
    public List<Animal> findAllByCompanyId(Long companyId) {
        return enrichAll(jpaRepository.findAllByCompany_Id(companyId).stream().map(mapper::toDomain).toList());
    }

    @Override
    public List<Animal> findByOwnerIdAndCompanyId(Long ownerId, Long companyId) {
        return enrichAll(jpaRepository.findAllByOwner_IdAndCompany_Id(ownerId, companyId)
            .stream().map(mapper::toDomain).toList());
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndCompany_Id(id, companyId).ifPresent(jpaRepository::delete);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }

    // --- Enriquecimiento del peso actual derivado del último WeightRecord habilitado ---

    private Animal enrich(Animal animal, Long companyId) {
        weightRecordJpaRepository.findLatestByAnimalId(animal.getId(), companyId)
            .ifPresent(p -> animal.applyCurrentWeight(p.getValue(), WeightType.valueOf(p.getUnit()), p.getMeasuredAt()));
        return animal;
    }

    // Una sola query por empresa (los animales pueden ser de distintas empresas en findAll) → sin N+1.
    private List<Animal> enrichAll(List<Animal> animals) {
        if (animals.isEmpty()) return animals;
        Map<Long, List<Animal>> byCompany = animals.stream()
            .collect(Collectors.groupingBy(a -> a.getCompany().id()));
        byCompany.forEach((companyId, group) -> {
            Map<Long, LatestWeightProjection> latest = weightRecordJpaRepository
                .findLatestByAnimalIds(companyId, group.stream().map(Animal::getId).toList())
                .stream().collect(Collectors.toMap(LatestWeightProjection::getAnimalId, Function.identity()));
            group.forEach(a -> {
                LatestWeightProjection p = latest.get(a.getId());
                if (p != null) a.applyCurrentWeight(p.getValue(), WeightType.valueOf(p.getUnit()), p.getMeasuredAt());
            });
        });
        return animals;
    }
}
