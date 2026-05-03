package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import java.util.List;

public interface ListOwnersUseCase {
    @RequiresPermission("admin.all")
    List<OwnerDto> listAll(AuthContext auth);
}
