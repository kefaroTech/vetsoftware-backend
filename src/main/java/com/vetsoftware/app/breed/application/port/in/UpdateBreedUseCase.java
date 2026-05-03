package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.breed.application.command.UpdateBreedCommand;
import com.vetsoftware.app.breed.application.dto.BreedDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateBreedUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    BreedDto execute(UpdateBreedCommand command);
}
