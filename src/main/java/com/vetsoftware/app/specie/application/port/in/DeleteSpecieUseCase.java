package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteSpecieUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
