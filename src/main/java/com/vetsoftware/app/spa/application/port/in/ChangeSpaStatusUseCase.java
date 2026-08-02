package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.command.ChangeSpaStatusCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeSpaStatusUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('spa.update') and @authz.isMyCompany(#command.companyId))")
    SpaDto execute(ChangeSpaStatusCommand command);
}
