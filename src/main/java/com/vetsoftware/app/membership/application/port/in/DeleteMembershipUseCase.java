package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteMembershipUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
