package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.city.application.dto.CityDto;
import java.util.List;

public interface ListCitiesUseCase {
    @RequiresPermission({"admin.all"})
    List<CityDto> listAll(AuthContext auth);
}
