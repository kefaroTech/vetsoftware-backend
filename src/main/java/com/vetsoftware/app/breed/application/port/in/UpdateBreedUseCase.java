package com.vetsoftware.app.breed.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.breed.application.command.UpdateBreedCommand;
import com.vetsoftware.app.breed.application.dto.BreedDto;

public interface UpdateBreedUseCase {
    @RequiresPermission("admin.all")
    BreedDto execute(UpdateBreedCommand command, AuthContext auth);
}
