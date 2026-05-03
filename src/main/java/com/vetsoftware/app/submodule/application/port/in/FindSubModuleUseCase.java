package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSubModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SubModuleDto findById(Long id);
}
