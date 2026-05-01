package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;

public interface CreateMembershipSubModuleUseCase {
    @RequiresPermission("admin.all")
    MembershipSubModuleDto execute(CreateMembershipSubModuleCommand command, AuthContext auth);
}
