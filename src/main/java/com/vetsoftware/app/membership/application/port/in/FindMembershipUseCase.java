package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.dto.MembershipDto;

public interface FindMembershipUseCase {
    @RequiresPermission("admin.all")
    MembershipDto findById(Long id, AuthContext auth);
}
