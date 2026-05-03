package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteBreedUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
