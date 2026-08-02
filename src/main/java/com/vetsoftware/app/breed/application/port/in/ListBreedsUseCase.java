package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import java.util.List;

public interface ListBreedsUseCase {
    List<BreedDto> listAll();
}
