package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateConsultationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('consultation.update') or hasRole('SYSTEM')")
    ConsultationDto execute(Long id);
}
