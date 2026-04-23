package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;

public interface CreateMembershipUseCase {
    @RequiresPermission("admin.all")
    MembershipDto execute(CreateMembershipCommand command, AuthContext auth);
}
