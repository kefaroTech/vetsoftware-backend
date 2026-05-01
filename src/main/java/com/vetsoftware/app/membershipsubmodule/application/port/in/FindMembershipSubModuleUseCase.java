package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;

public interface FindMembershipSubModuleUseCase {
    @RequiresPermission("admin.all")
    MembershipSubModuleDto findById(Long id, AuthContext auth);
}
