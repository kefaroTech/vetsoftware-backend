package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.command.UpdateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateModuleUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    ModuleDto execute(UpdateModuleCommand command);
}
