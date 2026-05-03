package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateMembershipUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    MembershipDto execute(UpdateMembershipCommand command);
}
