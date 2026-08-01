package com.vetsoftware.app.animalcolor.application.port.in;

import com.vetsoftware.app.animalcolor.application.command.UpdateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateAnimalColorUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    AnimalColorDto execute(UpdateAnimalColorCommand command);
}
