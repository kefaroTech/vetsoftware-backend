package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.in.ReactivateBreedUseCase;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "breed.reactivate")
@Service
public class ReactivateBreedService implements ReactivateBreedUseCase {
    private final BreedRepository repository;

    public ReactivateBreedService(BreedRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public BreedDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new BreedNotFoundException(id);
        return BreedDto.from(repository.findById(id)
            .orElseThrow(() -> new BreedNotFoundException(id)));
    }
}
