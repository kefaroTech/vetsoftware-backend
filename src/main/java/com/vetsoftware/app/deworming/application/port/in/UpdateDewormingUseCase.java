package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateDewormingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    DewormingDto execute(UpdateDewormingCommand command);
}
