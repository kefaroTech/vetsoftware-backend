package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.city.application.command.CreateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;

public interface CreateCityUseCase {
    @RequiresPermission({"admin.all"})
    CityDto execute(CreateCityCommand command, AuthContext auth);
}
