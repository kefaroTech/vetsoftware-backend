package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.breed.application.port.in.DeleteBreedUseCase;
import com.vetsoftware.app.breed.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.domain.BreedHasActiveChildrenException;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "breed.delete")
@Service
public class DeleteBreedService implements DeleteBreedUseCase {
    private final BreedRepository repository;
    private final AnimalChildrenQueryPort animalChildrenQueryPort;

    public DeleteBreedService(BreedRepository repository,
            AnimalChildrenQueryPort animalChildrenQueryPort) {
        this.repository = repository;
        this.animalChildrenQueryPort = animalChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new BreedNotFoundException(id));
        if (animalChildrenQueryPort.existsActiveByBreedId(id)) {
            throw new BreedHasActiveChildrenException(id, "animal");
        }
        repository.delete(id);
    }
}
