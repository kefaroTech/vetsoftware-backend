package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDayCareUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('daycare.create') and @authz.isMyCompany(#command.companyId)"
            + " and @authz.requireModuleWritable('GROOMING'))")
    DayCareDto execute(CreateDayCareCommand command);
}
