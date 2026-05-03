package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.specie.application.command.CreateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSpecieUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SpecieDto execute(CreateSpecieCommand command);
}
