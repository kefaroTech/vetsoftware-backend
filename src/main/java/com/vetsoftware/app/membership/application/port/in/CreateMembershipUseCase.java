package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateMembershipUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    MembershipDto execute(CreateMembershipCommand command);
}
