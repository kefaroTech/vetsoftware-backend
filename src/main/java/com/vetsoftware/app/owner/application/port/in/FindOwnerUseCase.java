package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindOwnerUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or hasAuthority('owner.read')")
    OwnerDto findById(Long id);
}
