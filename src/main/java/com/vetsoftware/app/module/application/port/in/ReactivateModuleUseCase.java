package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateModuleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('module.update')")
    ModuleDto execute(Long id);
}
