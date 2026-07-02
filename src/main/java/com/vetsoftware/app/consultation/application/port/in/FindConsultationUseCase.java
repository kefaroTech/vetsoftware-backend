package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindConsultationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('consultation.read') or hasRole('SYSTEM')")
    ConsultationDto findById(Long id, Long companyId);
}
