package com.vetsoftware.app.animalcolor.application.port.in;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;

public interface FindAnimalColorUseCase {
  AnimalColorDto findById(Long id);
}
