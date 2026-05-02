package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteSystemUserUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
