package com.vetsoftware.app.membershipmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.command.UpdateMembershipModuleCommand;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;

public interface UpdateMembershipModuleUseCase {
    @RequiresPermission("admin.all")
    MembershipModuleDto execute(UpdateMembershipModuleCommand command, AuthContext auth);
}
