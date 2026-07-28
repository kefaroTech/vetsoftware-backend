package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.port.in.DeleteAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColorHasActiveChildrenException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.color.delete")
@Service
public class DeleteAnimalColorService implements DeleteAnimalColorUseCase {
    private final AnimalColorRepository repository;
    private final AnimalChildrenQueryPort animalChildrenQueryPort;

    public DeleteAnimalColorService(
            AnimalColorRepository repository,
            AnimalChildrenQueryPort animalChildrenQueryPort) {
        this.repository = repository;
        this.animalChildrenQueryPort = animalChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new AnimalColorNotFoundException(id));
        if (animalChildrenQueryPort.existsActiveByAnimalColorId(id)) {
            throw new AnimalColorHasActiveChildrenException(id, "animal");
        }
        repository.delete(id);
    }
}
