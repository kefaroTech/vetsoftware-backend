package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface CreateAnimalUseCase {
    @RequiresPermission("admin.all")
    AnimalDto execute(CreateAnimalCommand command, AuthContext auth);
}
