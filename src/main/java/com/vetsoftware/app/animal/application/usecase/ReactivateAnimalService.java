package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.ReactivateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.reactivate")
@Service
public class ReactivateAnimalService implements ReactivateAnimalUseCase {
    private final AnimalRepository repository;

    public ReactivateAnimalService(AnimalRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AnimalDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new AnimalNotFoundException(id);
        return AnimalDto.from(repository.findById(id)
            .orElseThrow(() -> new AnimalNotFoundException(id)));
    }
}
