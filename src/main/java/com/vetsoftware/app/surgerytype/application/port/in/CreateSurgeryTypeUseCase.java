package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSurgeryTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('surgery.create') and @authz.isMyCompany(#command.companyId))")
    SurgeryTypeDto execute(CreateSurgeryTypeCommand command);
}
