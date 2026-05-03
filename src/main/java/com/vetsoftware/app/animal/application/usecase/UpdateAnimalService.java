package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.UpdateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.update")
@Service
public class UpdateAnimalService implements UpdateAnimalUseCase {
    private final AnimalRepository repository;
    private final SpecieQueryPort specieQueryPort;
    private final BreedQueryPort breedQueryPort;
    private final OwnerQueryPort ownerQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateAnimalService(AnimalRepository repository,
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
    @Transactional
    public AnimalDto execute(UpdateAnimalCommand command, AuthContext auth) {
        Animal animal = repository.findById(command.id())
            .orElseThrow(() -> new AnimalNotFoundException(command.id()));
        SpecieRef specie = specieQueryPort.findById(command.specieId())
            .orElseThrow(() -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        BreedRef breed = breedQueryPort.findById(command.breedId())
            .orElseThrow(() -> new IllegalArgumentException("Breed not found: " + command.breedId()));
        OwnerRef owner = ownerQueryPort.findById(command.ownerId())
            .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + command.ownerId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));

        animal.update(
            command.name(), command.code(), specie, breed, owner,
            command.gender(), command.weightType(), command.animalType(),
            command.reproductiveState(), command.color(), command.bod(),
            command.weight(), command.size(), command.deceased(), command.deceasedDate(),
            company
        );
        return AnimalDto.from(repository.save(animal));
    }
}
