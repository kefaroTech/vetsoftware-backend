package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.command.UpdateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateConsultationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    ConsultationDto execute(UpdateConsultationCommand command);
}
