package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.command.UpdateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSurgeryTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('surgery.update') or hasRole('SYSTEM')")
    SurgeryTypeDto execute(UpdateSurgeryTypeCommand command);
}
