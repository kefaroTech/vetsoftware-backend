package com.vetsoftware.app.membershipsubmodule.application.dto;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;

public record MembershipSummaryDto(Long id, String name) {
    public static MembershipSummaryDto from(MembershipRef ref) {
        return new MembershipSummaryDto(ref.id(), ref.name());
    }
}
