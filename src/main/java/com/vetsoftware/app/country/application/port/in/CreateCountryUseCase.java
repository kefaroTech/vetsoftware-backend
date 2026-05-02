package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.country.application.command.CreateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;

public interface CreateCountryUseCase {
    @RequiresPermission({"admin.all"})
    CountryDto execute(CreateCountryCommand command, AuthContext auth);
}
