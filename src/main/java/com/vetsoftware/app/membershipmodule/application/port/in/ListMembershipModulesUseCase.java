package com.vetsoftware.app.membershipmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;
import java.util.List;

public interface ListMembershipModulesUseCase {
    @RequiresPermission("admin.all")
    List<MembershipModuleDto> listAll(AuthContext auth);
}
