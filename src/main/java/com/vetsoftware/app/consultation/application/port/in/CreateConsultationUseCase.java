package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.command.CreateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateConsultationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('consultation.create') and @authz.isMyCompany(#command.companyId))")
    ConsultationDto execute(CreateConsultationCommand command);
}
