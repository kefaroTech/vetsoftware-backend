package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.port.in.DeleteAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animalColor.delete")
@Service
public class DeleteAnimalColorService implements DeleteAnimalColorUseCase {
    private final AnimalColorRepository repository;

    public DeleteAnimalColorService(AnimalColorRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new AnimalColorNotFoundException(id));
        repository.delete(id);
    }
}
