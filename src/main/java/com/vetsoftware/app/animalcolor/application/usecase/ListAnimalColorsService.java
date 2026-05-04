package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.ListAnimalColorsUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "animalColor.list")
@Service
public class ListAnimalColorsService implements ListAnimalColorsUseCase {
    private final AnimalColorRepository repository;

    public ListAnimalColorsService(AnimalColorRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AnimalColorDto> listAll() {
        return repository.findAll().stream().map(AnimalColorDto::from).toList();
    }
}
