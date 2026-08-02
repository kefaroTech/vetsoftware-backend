package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateMembershipSubModuleUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    MembershipSubModuleDto execute(UpdateMembershipSubModuleCommand command);
}
