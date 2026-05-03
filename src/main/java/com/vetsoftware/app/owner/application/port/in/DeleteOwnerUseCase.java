package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteOwnerUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
