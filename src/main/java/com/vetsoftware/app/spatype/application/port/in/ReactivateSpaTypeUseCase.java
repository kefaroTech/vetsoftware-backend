package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSpaTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('spatype.update') or hasRole('SYSTEM')")
    SpaTypeDto execute(Long id);
}
