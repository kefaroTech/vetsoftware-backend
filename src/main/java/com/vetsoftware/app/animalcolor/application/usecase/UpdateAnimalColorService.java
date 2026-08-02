package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.command.UpdateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.UpdateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.color.update")
@Service
public class UpdateAnimalColorService implements UpdateAnimalColorUseCase {
  private final AnimalColorRepository repository;

  public UpdateAnimalColorService(AnimalColorRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public AnimalColorDto execute(UpdateAnimalColorCommand command) {
    AnimalColor color =
        repository
            .findById(command.id())
            .orElseThrow(() -> new AnimalColorNotFoundException(command.id()));
    color.update(command.name());
    return AnimalColorDto.from(repository.save(color));
  }
}
