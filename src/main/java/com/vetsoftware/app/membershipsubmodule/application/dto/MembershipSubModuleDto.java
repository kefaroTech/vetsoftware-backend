package com.vetsoftware.app.membershipsubmodule.application.dto;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import java.time.LocalDateTime;

public record MembershipSubModuleDto(Long id,
                                      MembershipSummaryDto membership,
                                      SubModuleSummaryDto subModule,
                                      LocalDateTime createdDate,
                                      boolean enabled) {
    public static MembershipSubModuleDto from(MembershipSubModule membershipSubModule) {
        return new MembershipSubModuleDto(
            membershipSubModule.getId(),
            MembershipSummaryDto.from(membershipSubModule.getMembership()),
            SubModuleSummaryDto.from(membershipSubModule.getSubModule()),
            membershipSubModule.getCreatedDate(),
            membershipSubModule.isEnabled()
        );
    }
}
