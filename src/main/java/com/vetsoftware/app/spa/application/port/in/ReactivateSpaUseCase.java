package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSpaUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('spa.update') or hasRole('SYSTEM')")
    SpaDto execute(Long id);
}
