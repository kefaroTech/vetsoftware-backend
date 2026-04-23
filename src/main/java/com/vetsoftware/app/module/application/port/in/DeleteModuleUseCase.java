package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteModuleUseCase {
    @RequiresPermission("module.delete")
    void execute(Long id, AuthContext auth);
}
