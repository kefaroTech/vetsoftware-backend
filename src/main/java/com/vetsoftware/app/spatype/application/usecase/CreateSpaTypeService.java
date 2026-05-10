package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.command.CreateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.CreateSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "spa_type.create")
@Service
public class CreateSpaTypeService implements CreateSpaTypeUseCase {
    private final SpaTypeRepository repository;

    public CreateSpaTypeService(SpaTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public SpaTypeDto execute(CreateSpaTypeCommand command) {
        return SpaTypeDto.from(
                repository.save(SpaType.create(command.name(), command.description())));
    }
}
