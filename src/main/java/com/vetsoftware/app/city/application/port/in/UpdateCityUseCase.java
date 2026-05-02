package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.city.application.command.UpdateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;

public interface UpdateCityUseCase {
    @RequiresPermission({"admin.all"})
    CityDto execute(UpdateCityCommand command, AuthContext auth);
}
