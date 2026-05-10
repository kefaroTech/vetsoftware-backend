package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.spatype.application.command.CreateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSpaTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SpaTypeDto execute(CreateSpaTypeCommand command);
}
