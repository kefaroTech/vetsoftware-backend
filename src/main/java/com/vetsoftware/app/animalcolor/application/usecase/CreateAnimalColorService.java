package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.command.CreateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.CreateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "animalColor.create")
@Service
public class CreateAnimalColorService implements CreateAnimalColorUseCase {
    private final AnimalColorRepository repository;

    public CreateAnimalColorService(AnimalColorRepository repository) {
        this.repository = repository;
    }

    @Override
    public AnimalColorDto execute(CreateAnimalColorCommand command) {
        return AnimalColorDto.from(repository.save(AnimalColor.create(command.name())));
    }
}
