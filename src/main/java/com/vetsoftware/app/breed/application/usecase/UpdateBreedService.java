package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.breed.application.command.UpdateBreedCommand;
import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.in.UpdateBreedUseCase;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import com.vetsoftware.app.breed.domain.SpecieRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "breed.update")
@Service
public class UpdateBreedService implements UpdateBreedUseCase {
    private final BreedRepository repository;
    private final SpecieQueryPort specieQueryPort;

    public UpdateBreedService(BreedRepository repository, SpecieQueryPort specieQueryPort) {
        this.repository = repository;
        this.specieQueryPort = specieQueryPort;
    }

    @Override
    @Transactional
    public BreedDto execute(UpdateBreedCommand command, AuthContext auth) {
        Breed breed = repository.findById(command.id())
            .orElseThrow(() -> new BreedNotFoundException(command.id()));
        SpecieRef specie = specieQueryPort.findById(command.specieId())
            .orElseThrow(() -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        breed.update(command.name(), specie);
        return BreedDto.from(repository.save(breed));
    }
}
