package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateOwnerUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    OwnerDto execute(CreateOwnerCommand command);
}
