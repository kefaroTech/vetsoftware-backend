package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDayCareUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('dayCare.create') and @authz.isMyCompany(#command.companyId))")
    DayCareDto execute(CreateDayCareCommand command);
}
