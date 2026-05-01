package com.vetsoftware.app.membershipmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;

public interface FindMembershipModuleUseCase {
    @RequiresPermission("admin.all")
    MembershipModuleDto findById(Long id, AuthContext auth);
}
