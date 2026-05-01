package com.vetsoftware.app.membershipsubmodule.application.dto;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import java.time.LocalDateTime;

public record MembershipSubModuleDto(Long id, Long membershipId, Long subModuleId, LocalDateTime createdDate) {
    public static MembershipSubModuleDto from(MembershipSubModule membershipSubModule) {
        return new MembershipSubModuleDto(
            membershipSubModule.getId(),
            membershipSubModule.getMembershipId(),
            membershipSubModule.getSubModuleId(),
            membershipSubModule.getCreatedDate()
        );
    }
}
