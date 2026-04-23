package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;

public interface FindSubModuleUseCase {
    @RequiresPermission("admin.all")
    SubModuleDto findById(Long id, AuthContext auth);
}
