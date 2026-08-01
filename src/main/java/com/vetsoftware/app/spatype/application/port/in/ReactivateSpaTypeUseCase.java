package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSpaTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('spatype.update')")
    SpaTypeDto execute(Long id);
}
