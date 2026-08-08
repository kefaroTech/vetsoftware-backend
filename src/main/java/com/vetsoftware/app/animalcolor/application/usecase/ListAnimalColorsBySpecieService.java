package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.ListAnimalColorsBySpecieUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "animal.color.list.by.specie")
@Service
public class ListAnimalColorsBySpecieService implements ListAnimalColorsBySpecieUseCase {
    private final AnimalColorRepository repository;

    public ListAnimalColorsBySpecieService(AnimalColorRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AnimalColorDto> listBySpecie(Long specieId) {
        return repository.findBySpecieId(specieId).stream().map(AnimalColorDto::from).toList();
    }
}
