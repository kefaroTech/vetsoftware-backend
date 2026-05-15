package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.command.CreateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDewormingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('deworming.create') or hasRole('SYSTEM')")
    DewormingDto execute(CreateDewormingCommand command);
}
