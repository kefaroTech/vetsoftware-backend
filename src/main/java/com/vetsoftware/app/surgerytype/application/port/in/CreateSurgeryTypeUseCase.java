package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSurgeryTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('surgery.create') or hasRole('SYSTEM')")
    SurgeryTypeDto execute(CreateSurgeryTypeCommand command);
}
