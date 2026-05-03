package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.CreateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.SpecieRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "animal.create")
@Service
public class CreateAnimalService implements CreateAnimalUseCase {
    private final AnimalRepository repository;
    private final SpecieQueryPort specieQueryPort;
    private final BreedQueryPort breedQueryPort;
    private final OwnerQueryPort ownerQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateAnimalService(AnimalRepository repository,
                               SpecieQueryPort specieQueryPort,
                               BreedQueryPort breedQueryPort,
                               OwnerQueryPort ownerQueryPort,
                               CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.specieQueryPort = specieQueryPort;
        this.breedQueryPort = breedQueryPort;
        this.ownerQueryPort = ownerQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public AnimalDto execute(CreateAnimalCommand command) {
        SpecieRef specie = specieQueryPort.findById(command.specieId())
            .orElseThrow(() -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        BreedRef breed = breedQueryPort.findById(command.breedId())
            .orElseThrow(() -> new IllegalArgumentException("Breed not found: " + command.breedId()));
        OwnerRef owner = ownerQueryPort.findById(command.ownerId())
            .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + command.ownerId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));

        Animal animal = Animal.create(
            command.name(), command.code(), specie, breed, owner,
            command.gender(), command.weightType(), command.animalType(),
            command.reproductiveState(), command.color(), command.bod(),
            command.weight(), command.size(), command.deceased(), command.deceasedDate(),
            company
        );
        return AnimalDto.from(repository.save(animal));
    }
}
