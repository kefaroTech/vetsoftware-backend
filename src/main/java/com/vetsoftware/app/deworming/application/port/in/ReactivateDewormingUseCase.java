package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDewormingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('deworming.update')"
            + " and @authz.isMyCompany(#companyId))")
    DewormingDto execute(Long id, Long companyId);
}
