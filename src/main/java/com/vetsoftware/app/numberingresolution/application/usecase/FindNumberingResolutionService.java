package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.FindNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "numbering.resolution.find")
@Service
public class FindNumberingResolutionService implements FindNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;

    public FindNumberingResolutionService(NumberingResolutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public NumberingResolutionDto findById(Long id) {
        return NumberingResolutionDto.from(repository.findById(id)
                .orElseThrow(() -> new NumberingResolutionNotFoundException(id)));
    }
}
