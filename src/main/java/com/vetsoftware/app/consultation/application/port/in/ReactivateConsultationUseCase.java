package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateConsultationUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('consultation.update')")
  ConsultationDto execute(Long id, Long companyId);
}
