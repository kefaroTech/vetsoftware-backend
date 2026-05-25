package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSpaUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('spa.read') or hasRole('SYSTEM')")
    SpaDto findById(Long id);
}
