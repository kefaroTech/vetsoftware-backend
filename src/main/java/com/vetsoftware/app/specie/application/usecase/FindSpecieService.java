package com.vetsoftware.app.specie.application.usecase;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.in.FindSpecieUseCase;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "specie.find")
@Service
public class FindSpecieService implements FindSpecieUseCase {
    private final SpecieRepository repository;

    public FindSpecieService(SpecieRepository repository) {
        this.repository = repository;
    }

    @Override
    public SpecieDto findById(Long id) {
        return SpecieDto.from(repository.findById(id)
                .orElseThrow(() -> new SpecieNotFoundException(id)));
    }
}
