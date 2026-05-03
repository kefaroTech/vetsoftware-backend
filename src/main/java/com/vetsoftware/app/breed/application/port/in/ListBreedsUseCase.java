package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.breed.application.dto.BreedDto;
import java.util.List;

public interface ListBreedsUseCase {
    @RequiresPermission("admin.all")
    List<BreedDto> listAll(AuthContext auth);
}
