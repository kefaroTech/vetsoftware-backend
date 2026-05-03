package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateOwnerUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('owner.update'))")
    OwnerDto execute(UpdateOwnerCommand command);
}
