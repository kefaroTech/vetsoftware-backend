package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindMembershipSubModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    MembershipSubModuleDto findById(Long id);
}
