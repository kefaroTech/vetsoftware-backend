package com.vetsoftware.app.membership.application.dto;

import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.time.LocalDateTime;
import java.util.List;

public record MembershipDto(Long id, String name, MembershipStatus status, LocalDateTime createdDate, List<Long> moduleIds) {
    public static MembershipDto from(Membership membership) {
        return new MembershipDto(
            membership.getId(),
            membership.getName(),
            membership.getStatus(),
            membership.getCreatedDate(),
            membership.getModuleIds()
        );
    }
}
