package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteMembershipSubModuleUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
