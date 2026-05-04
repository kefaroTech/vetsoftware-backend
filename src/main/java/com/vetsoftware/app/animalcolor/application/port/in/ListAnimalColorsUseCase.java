package com.vetsoftware.app.animalcolor.application.port.in;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import java.util.List;

public interface ListAnimalColorsUseCase {
    List<AnimalColorDto> listAll();
}
