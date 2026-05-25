package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateMembershipUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('membership.update') or hasRole('SYSTEM')")
    MembershipDto execute(Long id);
}
