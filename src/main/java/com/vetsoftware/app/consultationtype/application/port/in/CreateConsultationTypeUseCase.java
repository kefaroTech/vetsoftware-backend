package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.command.CreateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateConsultationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    ConsultationTypeDto execute(CreateConsultationTypeCommand command);
}
