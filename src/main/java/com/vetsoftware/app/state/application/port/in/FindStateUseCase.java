package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.state.application.dto.StateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindStateUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    StateDto findById(Long id);
}
