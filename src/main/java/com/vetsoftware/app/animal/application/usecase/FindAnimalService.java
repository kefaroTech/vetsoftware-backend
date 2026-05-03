package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.FindAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "animal.find")
@Service
public class FindAnimalService implements FindAnimalUseCase {
    private final AnimalRepository repository;

    public FindAnimalService(AnimalRepository repository) {
        this.repository = repository;
    }

    @Override
    public AnimalDto findById(Long id) {
        return AnimalDto.from(repository.findById(id)
            .orElseThrow(() -> new AnimalNotFoundException(id)));
    }
}
