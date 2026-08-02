package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSpaUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('spa.update')")
    SpaDto execute(Long id);
}
