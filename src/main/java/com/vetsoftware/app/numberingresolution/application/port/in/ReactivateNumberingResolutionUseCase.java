package com.vetsoftware.app.numberingresolution.application.port.in;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateNumberingResolutionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('numberingResolution.delete')")
    NumberingResolutionDto execute(Long id);
}
