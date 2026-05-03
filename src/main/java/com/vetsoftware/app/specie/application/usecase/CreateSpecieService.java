package com.vetsoftware.app.specie.application.usecase;

import com.vetsoftware.app.specie.application.command.CreateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.in.CreateSpecieUseCase;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.Specie;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "specie.create")
@Service
public class CreateSpecieService implements CreateSpecieUseCase {
    private final SpecieRepository repository;

    public CreateSpecieService(SpecieRepository repository) {
        this.repository = repository;
    }

    @Override
    public SpecieDto execute(CreateSpecieCommand command) {
        return SpecieDto.from(repository.save(Specie.create(command.name())));
    }
}
