package com.vetsoftware.app.state.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteStateUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
