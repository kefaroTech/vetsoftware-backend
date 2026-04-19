package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;

public interface FindMembershipUseCase {
    MembershipDto findById(Long id);
}
