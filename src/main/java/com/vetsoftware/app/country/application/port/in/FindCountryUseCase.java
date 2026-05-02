package com.vetsoftware.app.country.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.country.application.dto.CountryDto;

public interface FindCountryUseCase {
    @RequiresPermission({"admin.all"})
    CountryDto findById(Long id, AuthContext auth);
}
