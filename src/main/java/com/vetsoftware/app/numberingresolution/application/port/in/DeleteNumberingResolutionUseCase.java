package com.vetsoftware.app.numberingresolution.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteNumberingResolutionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('electronicbilling.delete')")
    void execute(Long id);
}
