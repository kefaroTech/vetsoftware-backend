package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.port.in.DeleteAnimalUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.delete")
@Service
public class DeleteAnimalService implements DeleteAnimalUseCase {
    private final AnimalRepository repository;

    public DeleteAnimalService(AnimalRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new AnimalNotFoundException(id));
        repository.delete(id);
    }
}
