package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.specie.application.dto.SpecieDto;

public interface FindSpecieUseCase {
    @RequiresPermission("admin.all")
    SpecieDto findById(Long id, AuthContext auth);
}
