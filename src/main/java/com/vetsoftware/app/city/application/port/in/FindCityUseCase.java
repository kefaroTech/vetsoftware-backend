package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.city.application.dto.CityDto;

public interface FindCityUseCase {
    @RequiresPermission({"admin.all"})
    CityDto findById(Long id, AuthContext auth);
}
