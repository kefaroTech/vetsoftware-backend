package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.command.UpdateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.UpdateSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaType;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.type.update")
@Service
public class UpdateSpaTypeService implements UpdateSpaTypeUseCase {
    private final SpaTypeRepository repository;

    public UpdateSpaTypeService(SpaTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SpaTypeDto execute(UpdateSpaTypeCommand command) {
        SpaType spaType = repository.findById(command.id())
                .orElseThrow(() -> new SpaTypeNotFoundException(command.id()));
        spaType.update(command.name(), command.description());
        return SpaTypeDto.from(repository.save(spaType));
    }
}
