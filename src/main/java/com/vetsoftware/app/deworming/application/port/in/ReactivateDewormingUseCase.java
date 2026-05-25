package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDewormingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('deworming.update') or hasRole('SYSTEM')")
    DewormingDto execute(Long id);
}
