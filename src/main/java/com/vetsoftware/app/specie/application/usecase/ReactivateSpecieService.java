package com.vetsoftware.app.specie.application.usecase;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.in.ReactivateSpecieUseCase;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "specie.reactivate")
@Service
public class ReactivateSpecieService implements ReactivateSpecieUseCase {
    private final SpecieRepository repository;

    public ReactivateSpecieService(SpecieRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SpecieDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new SpecieNotFoundException(id);
        return SpecieDto
                .from(repository.findById(id).orElseThrow(() -> new SpecieNotFoundException(id)));
    }
}
