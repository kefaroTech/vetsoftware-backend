package com.vetsoftware.app.membershipsubmodule.application.port.in;

import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateMembershipSubModuleUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  MembershipSubModuleDto execute(CreateMembershipSubModuleCommand command);
}
