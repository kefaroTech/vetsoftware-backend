package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.command.UpdateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.UpdateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.color.update")
@Service
public class UpdateAnimalColorService implements UpdateAnimalColorUseCase {
    private final AnimalColorRepository repository;
    private final SpecieQueryPort specieQueryPort;

    public UpdateAnimalColorService(AnimalColorRepository repository,
            SpecieQueryPort specieQueryPort) {
        this.repository = repository;
        this.specieQueryPort = specieQueryPort;
    }

    @Override
    @Transactional
    public AnimalColorDto execute(UpdateAnimalColorCommand command) {
        AnimalColor color = repository.findById(command.id())
                .orElseThrow(() -> new AnimalColorNotFoundException(command.id()));
        SpecieRef specie = specieQueryPort.findById(command.specieId()).orElseThrow(
                () -> new IllegalArgumentException("Specie not found: " + command.specieId()));
        color.update(command.name(), specie);
        return AnimalColorDto.from(repository.save(color));
    }
}
