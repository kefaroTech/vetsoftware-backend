package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSurgeryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('surgery.read') or hasRole('SYSTEM')")
    SurgeryDto findById(Long id);
}
