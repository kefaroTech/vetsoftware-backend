package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.module.application.dto.ModuleDto;

public interface FindModuleUseCase {
    @RequiresPermission({"admin.all"})
    ModuleDto findById(Long id, AuthContext auth);
}
