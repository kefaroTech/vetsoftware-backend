package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSurgeryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SurgeryDto execute(UpdateSurgeryCommand command);
}
