package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.in.ListBreedsBySpecieUseCase;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "breed.listBySpecie")
@Service
public class ListBreedsBySpecieService implements ListBreedsBySpecieUseCase {
    private final BreedRepository repository;

    public ListBreedsBySpecieService(BreedRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BreedDto> listBySpecie(Long specieId) {
        return repository.findBySpecieId(specieId).stream().map(BreedDto::from).toList();
    }
}
