package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSubModuleUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SubModuleDto execute(CreateSubModuleCommand command);
}
