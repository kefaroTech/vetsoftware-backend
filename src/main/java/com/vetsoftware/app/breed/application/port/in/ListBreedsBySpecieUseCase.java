package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import java.util.List;

public interface ListBreedsBySpecieUseCase {
    List<BreedDto> listBySpecie(Long specieId);
}
