package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteSubModuleUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
