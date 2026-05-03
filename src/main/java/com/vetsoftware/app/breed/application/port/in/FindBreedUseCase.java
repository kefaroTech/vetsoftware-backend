package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.breed.application.dto.BreedDto;

public interface FindBreedUseCase {
    @RequiresPermission("admin.all")
    BreedDto findById(Long id, AuthContext auth);
}
