package com.vetsoftware.app.numberingresolution.application.port.in;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindNumberingResolutionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('electronicbilling.read') and @authz.isMyCompany(#companyId))")
    NumberingResolutionDto findById(Long id, Long companyId);
}
