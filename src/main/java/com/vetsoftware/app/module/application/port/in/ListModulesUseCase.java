package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import java.util.List;

public interface ListModulesUseCase {
    @RequiresPermission("module.list")
    List<ModuleDto> listAll(AuthContext auth);
}
