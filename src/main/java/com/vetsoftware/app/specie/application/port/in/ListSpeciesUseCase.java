package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import java.util.List;

public interface ListSpeciesUseCase {
    @RequiresPermission("admin.all")
    List<SpecieDto> listAll(AuthContext auth);
}
