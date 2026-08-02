package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.command.UpdateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateConsultationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('consultation.update') and @authz.isMyCompany(#command.companyId))")
    ConsultationDto execute(UpdateConsultationCommand command);
}
