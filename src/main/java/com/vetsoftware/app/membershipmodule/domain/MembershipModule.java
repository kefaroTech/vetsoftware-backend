package com.vetsoftware.app.membershipmodule.domain;

import java.time.LocalDateTime;

public class MembershipModule {
    private Long id;
    private Long membershipId;
    private Long subModuleId;
    private final LocalDateTime createdDate;

    public MembershipModule(Long id, Long membershipId, Long subModuleId, LocalDateTime createdDate) {
        if (membershipId == null) throw new IllegalArgumentException("membershipId is required");
        if (subModuleId == null) throw new IllegalArgumentException("subModuleId is required");
        this.id = id;
        this.membershipId = membershipId;
        this.subModuleId = subModuleId;
        this.createdDate = createdDate;
    }

    public static MembershipModule create(Long membershipId, Long subModuleId) {
        return new MembershipModule(null, membershipId, subModuleId, LocalDateTime.now());
    }

    public void update(Long membershipId, Long subModuleId) {
        if (membershipId == null) throw new IllegalArgumentException("membershipId is required");
        if (subModuleId == null) throw new IllegalArgumentException("subModuleId is required");
        this.membershipId = membershipId;
        this.subModuleId = subModuleId;
    }

    public Long getId() { return id; }
    public Long getMembershipId() { return membershipId; }
    public Long getSubModuleId() { return subModuleId; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
