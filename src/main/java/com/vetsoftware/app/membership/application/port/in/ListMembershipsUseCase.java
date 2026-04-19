package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import java.util.List;

public interface ListMembershipsUseCase {
    List<MembershipDto> listAll();
}
