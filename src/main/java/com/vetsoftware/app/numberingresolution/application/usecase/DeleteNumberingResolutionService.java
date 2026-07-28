package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.port.in.DeleteNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "numbering.resolution.delete")
@Service
public class DeleteNumberingResolutionService implements DeleteNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;

    public DeleteNumberingResolutionService(NumberingResolutionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new NumberingResolutionNotFoundException(id));
        repository.delete(id);
    }
}
