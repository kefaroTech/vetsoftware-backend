package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSystemUserUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SystemUserDto execute(CreateSystemUserCommand command);
}
