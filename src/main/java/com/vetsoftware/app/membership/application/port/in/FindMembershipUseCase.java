package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindMembershipUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    MembershipDto findById(Long id);
}
