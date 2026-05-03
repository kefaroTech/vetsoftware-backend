package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import java.util.List;

public interface ListAnimalsUseCase {
    @RequiresPermission("admin.all")
    List<AnimalDto> listAll(AuthContext auth);
}
