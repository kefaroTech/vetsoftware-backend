package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSurgeryTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('surgerytype.update') or hasRole('SYSTEM')")
    SurgeryTypeDto execute(Long id);
}
