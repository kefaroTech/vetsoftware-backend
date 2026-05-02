package com.vetsoftware.app.city.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.city.application.dto.CityDto;
import java.util.List;

public interface ListCitiesByStateUseCase {
    @RequiresPermission({"admin.all"})
    List<CityDto> listByState(Long stateId, AuthContext auth);
}
