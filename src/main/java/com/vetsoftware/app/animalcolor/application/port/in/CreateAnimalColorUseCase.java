package com.vetsoftware.app.animalcolor.application.port.in;

import com.vetsoftware.app.animalcolor.application.command.CreateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateAnimalColorUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    AnimalColorDto execute(CreateAnimalColorCommand command);
}
