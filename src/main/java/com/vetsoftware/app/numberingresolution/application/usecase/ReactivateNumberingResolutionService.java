package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.ReactivateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "numbering.resolution.reactivate")
@Service
public class ReactivateNumberingResolutionService implements ReactivateNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;

    public ReactivateNumberingResolutionService(NumberingResolutionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public NumberingResolutionDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new NumberingResolutionNotFoundException(id);
        return NumberingResolutionDto.from(repository.findById(id)
                .orElseThrow(() -> new NumberingResolutionNotFoundException(id)));
    }
}
