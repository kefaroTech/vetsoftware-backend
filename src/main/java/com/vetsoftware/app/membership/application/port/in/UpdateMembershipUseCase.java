package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;

public interface UpdateMembershipUseCase {
    @RequiresPermission("admin.all")
    MembershipDto execute(UpdateMembershipCommand command, AuthContext auth);
}
