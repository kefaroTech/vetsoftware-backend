package com.vetsoftware.app.numberingresolution.application.port.in;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateNumberingResolutionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('electronicbilling.delete')"
            + " and @authz.isMyCompany(#companyId))")
    NumberingResolutionDto execute(Long id, Long companyId);
}
