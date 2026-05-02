package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.country.application.command.UpdateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;

public interface UpdateCountryUseCase {
    @RequiresPermission({"admin.all"})
    CountryDto execute(UpdateCountryCommand command, AuthContext auth);
}
