package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.command.ChangeSurgeryStatusCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeSurgeryStatusUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('surgery.update') and @authz.isMyCompany(#command.companyId))")
    SurgeryDto execute(ChangeSurgeryStatusCommand command);
}
