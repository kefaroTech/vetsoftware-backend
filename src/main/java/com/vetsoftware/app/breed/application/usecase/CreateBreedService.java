package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.breed.application.command.CreateBreedCommand;
import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.in.CreateBreedUseCase;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.SpecieRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "breed.create")
@Service
public class CreateBreedService implements CreateBreedUseCase {
    private final BreedRepository repository;
    private final SpecieQueryPort specieQueryPort;

    public CreateBreedService(BreedRepository repository, SpecieQueryPort specieQueryPort) {
        this.repository = repository;
        this.specieQueryPort = specieQueryPort;
    }

    @Override
    public BreedDto execute(CreateBreedCommand command) {
        SpecieRef specie = specieQueryPort.findById(command.specieId())
            .orElseThrow(() -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        Breed breed = Breed.create(command.name(), specie);
        return BreedDto.from(repository.save(breed));
    }
}
