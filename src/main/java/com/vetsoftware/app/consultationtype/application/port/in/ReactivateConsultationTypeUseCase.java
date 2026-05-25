package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateConsultationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('consultationtype.update') or hasRole('SYSTEM')")
    ConsultationTypeDto execute(Long id);
}
