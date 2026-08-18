package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDayCareUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('daycare.update')"
            + " and @authz.isMyCompany(#companyId))")
    DayCareDto execute(Long id, Long companyId);
}
