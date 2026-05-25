package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.command.ChangeSpaStatusCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeSpaStatusUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SpaDto execute(ChangeSpaStatusCommand command);
}
