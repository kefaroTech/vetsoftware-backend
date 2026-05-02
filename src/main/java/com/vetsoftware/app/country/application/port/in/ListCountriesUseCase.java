package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.country.application.dto.CountryDto;
import java.util.List;

public interface ListCountriesUseCase {
    @RequiresPermission({"admin.all"})
    List<CountryDto> listAll(AuthContext auth);
}
