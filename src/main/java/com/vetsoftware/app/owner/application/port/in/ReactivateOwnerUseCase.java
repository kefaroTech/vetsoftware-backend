package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateOwnerUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('owner.update') or hasRole('SYSTEM')")
    OwnerDto execute(Long id);
}
