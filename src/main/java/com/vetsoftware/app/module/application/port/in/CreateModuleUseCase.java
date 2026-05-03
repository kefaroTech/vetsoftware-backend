package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.command.CreateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    ModuleDto execute(CreateModuleCommand command);
}
