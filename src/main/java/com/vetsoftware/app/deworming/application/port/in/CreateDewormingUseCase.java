package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.command.CreateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDewormingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('deworming.create')")
    DewormingDto execute(CreateDewormingCommand command);
}
