package com.vetsoftware.app.deworming.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDewormingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('deworming.delete')")
    void execute(Long id);
}
