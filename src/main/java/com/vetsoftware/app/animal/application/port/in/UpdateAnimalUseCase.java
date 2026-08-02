package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.update'))")
    AnimalDto execute(UpdateAnimalCommand command);
}
