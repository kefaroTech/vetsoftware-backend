package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDayCareUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('dayCare.read') or hasRole('SYSTEM')")
    DayCareDto findById(Long id);
}
