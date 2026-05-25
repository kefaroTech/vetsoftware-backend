package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDayCareUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('daycare.update') or hasRole('SYSTEM')")
    DayCareDto execute(Long id);
}
