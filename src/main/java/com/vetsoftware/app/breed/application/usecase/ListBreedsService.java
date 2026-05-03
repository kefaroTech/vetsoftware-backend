package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.in.ListBreedsUseCase;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "breed.list")
@Service
public class ListBreedsService implements ListBreedsUseCase {
    private final BreedRepository repository;

    public ListBreedsService(BreedRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BreedDto> listAll() {
        return repository.findAll().stream().map(BreedDto::from).toList();
    }
}
