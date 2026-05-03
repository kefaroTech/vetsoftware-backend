package com.vetsoftware.app.specie.application.usecase;

import com.vetsoftware.app.specie.application.port.in.DeleteSpecieUseCase;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "specie.delete")
@Service
public class DeleteSpecieService implements DeleteSpecieUseCase {
    private final SpecieRepository repository;

    public DeleteSpecieService(SpecieRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SpecieNotFoundException(id));
        repository.delete(id);
    }
}
