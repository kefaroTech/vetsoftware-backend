package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSubModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('submodule.update') or hasRole('SYSTEM')")
    SubModuleDto execute(Long id);
}
