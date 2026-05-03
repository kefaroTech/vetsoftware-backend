package com.vetsoftware.app.membershipsubmodule.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteMembershipSubModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
