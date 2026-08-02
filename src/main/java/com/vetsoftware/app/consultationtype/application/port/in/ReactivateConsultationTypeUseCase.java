package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateConsultationTypeUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('consultationtype.update')")
  ConsultationTypeDto execute(Long id);
}
