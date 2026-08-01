package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindConsultationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('consultation.read')")
    ConsultationDto findById(Long id, Long companyId);
}
