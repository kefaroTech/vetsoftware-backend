package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.UpdateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalColorQueryPort;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalColorRef;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.SpecieRef;
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
    private final AnimalColorQueryPort animalColorQueryPort;

    public UpdateAnimalService(AnimalRepository repository, SpecieQueryPort specieQueryPort,
            BreedQueryPort breedQueryPort, OwnerQueryPort ownerQueryPort,
            CompanyQueryPort companyQueryPort, AnimalColorQueryPort animalColorQueryPort) {
        this.repository = repository;
        this.specieQueryPort = specieQueryPort;
        this.breedQueryPort = breedQueryPort;
        this.ownerQueryPort = ownerQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.animalColorQueryPort = animalColorQueryPort;
    }

    @Override
    @Transactional
    public AnimalDto execute(UpdateAnimalCommand command) {
        Animal animal = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new AnimalNotFoundException(command.id()));
        SpecieRef specie = specieQueryPort.findById(command.specieId()).orElseThrow(
                () -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        BreedRef breed = breedQueryPort.findById(command.breedId()).orElseThrow(
                () -> new IllegalArgumentException("Breed not found: " + command.breedId()));
        OwnerRef owner = ownerQueryPort.findByIdAndCompanyId(command.ownerId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Owner not found: " + command.ownerId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        AnimalColorRef color = animalColorQueryPort.findById(command.colorId()).orElseThrow(
                () -> new IllegalArgumentException("AnimalColor not found: " + command.colorId()));

        // command.weight() se ignora deliberadamente: el peso se gestiona como serie
        // temporal vía
        // /animals/{id}/weight-records, no desde el update del animal. Ver
        // WeightRecord.
        animal.update(command.name(), command.code(), specie, breed, owner, command.gender(),
                command.weightType(), command.animalType(), command.reproductiveState(), color,
                command.bod(), command.size(), command.deceased(), command.deceasedDate(), company);
        return AnimalDto.from(repository.save(animal));
    }
}
