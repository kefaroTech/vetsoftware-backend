package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDewormingUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('deworming.read') or hasRole('SYSTEM')")
    DewormingDto findById(Long id);
}
