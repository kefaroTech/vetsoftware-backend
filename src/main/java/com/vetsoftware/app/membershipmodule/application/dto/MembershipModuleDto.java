package com.vetsoftware.app.membershipmodule.application.dto;

import com.vetsoftware.app.membershipmodule.domain.MembershipModule;
import java.time.LocalDateTime;

public record MembershipModuleDto(Long id, Long membershipId, Long subModuleId, LocalDateTime createdDate) {
    public static MembershipModuleDto from(MembershipModule membershipModule) {
        return new MembershipModuleDto(
            membershipModule.getId(),
            membershipModule.getMembershipId(),
            membershipModule.getSubModuleId(),
            membershipModule.getCreatedDate()
        );
    }
}
