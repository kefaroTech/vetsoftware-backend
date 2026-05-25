package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.state.application.dto.StateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateStateUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('state.update') or hasRole('SYSTEM')")
    StateDto execute(Long id);
}
