package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteCountryUseCase {
    @RequiresPermission({"admin.all"})
    void execute(Long id, AuthContext auth);
}
