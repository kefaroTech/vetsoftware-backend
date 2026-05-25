package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSystemUserUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('systemuser.update') or hasRole('SYSTEM')")
    SystemUserDto execute(Long id);
}
