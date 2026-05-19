package com.vetsoftware.app.deworming.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDewormingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('deworming.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
