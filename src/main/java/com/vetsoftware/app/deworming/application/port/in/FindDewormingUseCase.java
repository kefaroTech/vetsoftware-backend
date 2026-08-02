package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDewormingUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('deworming.read') and @authz.isMyCompany(#companyId))")
    DewormingDto findById(Long id, Long companyId);
}
