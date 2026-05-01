package com.vetsoftware.app.membershipmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.command.CreateMembershipModuleCommand;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;

public interface CreateMembershipModuleUseCase {
    @RequiresPermission("admin.all")
    MembershipModuleDto execute(CreateMembershipModuleCommand command, AuthContext auth);
}
