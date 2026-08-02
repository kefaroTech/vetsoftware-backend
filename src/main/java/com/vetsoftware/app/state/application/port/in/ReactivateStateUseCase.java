package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.state.application.dto.StateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateStateUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('state.update')")
    StateDto execute(Long id);
}
