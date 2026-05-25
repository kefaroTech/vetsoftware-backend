package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('module.update') or hasRole('SYSTEM')")
    ModuleDto execute(Long id);
}
