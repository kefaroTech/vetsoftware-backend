package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.owner.application.dto.OwnerDto;

public interface FindOwnerUseCase {
    @RequiresPermission("admin.all")
    OwnerDto findById(Long id, AuthContext auth);
}
