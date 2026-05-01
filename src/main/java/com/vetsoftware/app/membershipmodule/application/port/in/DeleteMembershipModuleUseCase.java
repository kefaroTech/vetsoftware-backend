package com.vetsoftware.app.membershipmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteMembershipModuleUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
