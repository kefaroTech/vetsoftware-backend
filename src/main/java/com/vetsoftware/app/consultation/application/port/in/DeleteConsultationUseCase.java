package com.vetsoftware.app.consultation.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteConsultationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
