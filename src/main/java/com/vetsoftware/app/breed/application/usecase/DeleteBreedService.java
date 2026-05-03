package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.breed.application.port.in.DeleteBreedUseCase;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "breed.delete")
@Service
public class DeleteBreedService implements DeleteBreedUseCase {
    private final BreedRepository repository;

    public DeleteBreedService(BreedRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new BreedNotFoundException(id));
        repository.delete(id);
    }
}
