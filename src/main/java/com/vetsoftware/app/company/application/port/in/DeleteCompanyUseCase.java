package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteCompanyUseCase {
    @RequiresPermission({"admin.all","company.delete"})
    void execute(Long id, AuthContext auth);
}
