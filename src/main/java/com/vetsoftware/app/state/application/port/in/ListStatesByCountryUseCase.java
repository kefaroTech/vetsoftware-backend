package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.state.application.dto.StateDto;
import java.util.List;

public interface ListStatesByCountryUseCase {
    @RequiresPermission({"admin.all"})
    List<StateDto> listByCountry(Long countryId, AuthContext auth);
}
