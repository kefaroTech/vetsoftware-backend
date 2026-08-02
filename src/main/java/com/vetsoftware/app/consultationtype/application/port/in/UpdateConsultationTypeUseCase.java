package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.command.UpdateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateConsultationTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    ConsultationTypeDto execute(UpdateConsultationTypeCommand command);
}
