package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;

public interface UpdateMembershipUseCase {
    MembershipDto execute(UpdateMembershipCommand command);
}
