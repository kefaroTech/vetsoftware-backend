package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSystemUserUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SystemUserDto findById(Long id);
}
