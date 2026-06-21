package com.vetsoftware.app.numberingresolution.application.port.in;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindNumberingResolutionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('electronicbilling.read')")
    NumberingResolutionDto findById(Long id);
}
