package com.vetsoftware.app.submodule.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSubModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
