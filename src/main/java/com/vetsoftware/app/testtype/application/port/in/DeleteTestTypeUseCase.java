package com.vetsoftware.app.testtype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteTestTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
