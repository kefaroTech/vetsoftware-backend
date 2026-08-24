package com.vetsoftware.app.dunning.application.port.in;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDunningEventUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('dunningEvent.read')"
            + " and @authz.isMyCompany(#companyId))")
    DunningEventDto findById(Long id, Long companyId);
}
