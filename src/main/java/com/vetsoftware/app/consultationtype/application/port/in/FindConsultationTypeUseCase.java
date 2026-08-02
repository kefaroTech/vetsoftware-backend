package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindConsultationTypeUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  ConsultationTypeDto findById(Long id);
}
