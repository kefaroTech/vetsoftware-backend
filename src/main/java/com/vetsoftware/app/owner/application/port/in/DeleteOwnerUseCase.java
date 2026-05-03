package com.vetsoftware.app.owner.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteOwnerUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('owner.delete'))")
    void execute(Long id);
}
