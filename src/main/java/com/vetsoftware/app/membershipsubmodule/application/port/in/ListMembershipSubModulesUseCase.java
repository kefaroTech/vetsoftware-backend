package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import java.util.List;

public interface ListMembershipSubModulesUseCase {
    @RequiresPermission("admin.all")
    List<MembershipSubModuleDto> listAll(AuthContext auth);
}
