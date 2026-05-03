package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMembershipSubModulesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<MembershipSubModuleDto> listAll();
}
