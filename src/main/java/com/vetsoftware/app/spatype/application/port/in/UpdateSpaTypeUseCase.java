package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.spatype.application.command.UpdateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSpaTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SpaTypeDto execute(UpdateSpaTypeCommand command);
}
