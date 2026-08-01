package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSystemUserUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('systemuser.update')")
    SystemUserDto execute(Long id);
}
