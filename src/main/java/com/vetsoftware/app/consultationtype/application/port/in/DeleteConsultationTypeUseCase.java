package com.vetsoftware.app.consultationtype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteConsultationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
