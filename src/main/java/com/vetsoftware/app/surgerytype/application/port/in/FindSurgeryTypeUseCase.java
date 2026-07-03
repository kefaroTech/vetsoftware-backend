package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSurgeryTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('surgery.read') or hasRole('SYSTEM')")
    SurgeryTypeDto findById(Long id);
}
