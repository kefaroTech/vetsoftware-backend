package com.vetsoftware.app.specie.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.specie.application.command.UpdateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.in.UpdateSpecieUseCase;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.Specie;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "specie.update")
@Service
public class UpdateSpecieService implements UpdateSpecieUseCase {
    private final SpecieRepository repository;

    public UpdateSpecieService(SpecieRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SpecieDto execute(UpdateSpecieCommand command, AuthContext auth) {
        Specie specie = repository.findById(command.id())
                .orElseThrow(() -> new SpecieNotFoundException(command.id()));
        specie.update(command.name());
        return SpecieDto.from(repository.save(specie));
    }
}
