package com.vetsoftware.app.specie.application.usecase;

import com.vetsoftware.app.specie.application.port.in.DeleteSpecieUseCase;
import com.vetsoftware.app.specie.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.specie.application.port.out.BreedChildrenQueryPort;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.SpecieHasActiveChildrenException;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "specie.delete")
@Service
public class DeleteSpecieService implements DeleteSpecieUseCase {
    private final SpecieRepository repository;
    private final BreedChildrenQueryPort breedChildrenQueryPort;
    private final AnimalChildrenQueryPort animalChildrenQueryPort;

    public DeleteSpecieService(SpecieRepository repository,
            BreedChildrenQueryPort breedChildrenQueryPort,
            AnimalChildrenQueryPort animalChildrenQueryPort) {
        this.repository = repository;
        this.breedChildrenQueryPort = breedChildrenQueryPort;
        this.animalChildrenQueryPort = animalChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SpecieNotFoundException(id));
        if (breedChildrenQueryPort.existsActiveBySpecieId(id)) {
            throw new SpecieHasActiveChildrenException(id, "breed");
        }
        if (animalChildrenQueryPort.existsActiveBySpecieId(id)) {
            throw new SpecieHasActiveChildrenException(id, "animal");
        }
        repository.delete(id);
    }
}
