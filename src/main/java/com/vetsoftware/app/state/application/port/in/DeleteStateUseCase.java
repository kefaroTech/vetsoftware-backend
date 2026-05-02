package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteStateUseCase {
    @RequiresPermission({"admin.all"})
    void execute(Long id, AuthContext auth);
}
