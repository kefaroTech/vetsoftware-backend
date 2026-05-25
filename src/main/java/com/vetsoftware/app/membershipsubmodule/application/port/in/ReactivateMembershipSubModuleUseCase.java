package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateMembershipSubModuleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('membership_sub_module.update') or hasRole('SYSTEM')")
    MembershipSubModuleDto execute(Long id);
}
