package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import java.util.List;

public interface ListMembershipsUseCase {
    @RequiresPermission("admin.all")
    List<MembershipDto> listAll(AuthContext auth);
}
