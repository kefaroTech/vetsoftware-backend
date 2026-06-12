package com.vetsoftware.app.economicactivity.application.port.in;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateEconomicActivityUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    EconomicActivityDto execute(Long id);
}
