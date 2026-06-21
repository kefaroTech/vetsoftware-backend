package com.vetsoftware.app.numberingresolution.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteNumberingResolutionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('electronicbilling.delete')")
    void execute(Long id);
}
