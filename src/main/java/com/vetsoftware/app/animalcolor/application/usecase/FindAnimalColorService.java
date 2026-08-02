package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.FindAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "animal.color.find")
@Service
public class FindAnimalColorService implements FindAnimalColorUseCase {
  private final AnimalColorRepository repository;

  public FindAnimalColorService(AnimalColorRepository repository) {
    this.repository = repository;
  }

  @Override
  public AnimalColorDto findById(Long id) {
    return AnimalColorDto.from(
        repository.findById(id).orElseThrow(() -> new AnimalColorNotFoundException(id)));
  }
}
