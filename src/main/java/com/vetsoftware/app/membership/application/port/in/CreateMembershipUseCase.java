package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;

public interface CreateMembershipUseCase {
    MembershipDto execute(CreateMembershipCommand command);
}
