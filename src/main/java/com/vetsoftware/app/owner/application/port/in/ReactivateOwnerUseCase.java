package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateOwnerUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('owner.update') and @authz.isMyCompany(#companyId))")
    OwnerDto execute(Long id, Long companyId);
}
