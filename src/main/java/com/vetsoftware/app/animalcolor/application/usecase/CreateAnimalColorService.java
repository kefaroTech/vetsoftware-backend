package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.command.CreateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.CreateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "animal.color.create")
@Service
public class CreateAnimalColorService implements CreateAnimalColorUseCase {
    private final AnimalColorRepository repository;
    private final SpecieQueryPort specieQueryPort;

    public CreateAnimalColorService(AnimalColorRepository repository,
            SpecieQueryPort specieQueryPort) {
        this.repository = repository;
        this.specieQueryPort = specieQueryPort;
    }

    @Override
    public AnimalColorDto execute(CreateAnimalColorCommand command) {
        SpecieRef specie = specieQueryPort.findById(command.specieId()).orElseThrow(
                () -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        return AnimalColorDto.from(repository.save(AnimalColor.create(command.name(), specie)));
    }
}
