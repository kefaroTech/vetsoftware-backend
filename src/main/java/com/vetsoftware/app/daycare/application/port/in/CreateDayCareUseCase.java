package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDayCareUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('dayCare.create') or hasRole('SYSTEM')")
    DayCareDto execute(CreateDayCareCommand command);
}
