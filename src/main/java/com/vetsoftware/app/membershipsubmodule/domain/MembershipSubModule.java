package com.vetsoftware.app.membershipsubmodule.domain;

import java.time.LocalDateTime;

public class MembershipSubModule {
    private Long id;
    private MembershipRef membership;
    private SubModuleRef subModule;
    private final LocalDateTime createdDate;

    public MembershipSubModule(Long id, MembershipRef membership, SubModuleRef subModule, LocalDateTime createdDate) {
        if (membership == null) throw new IllegalArgumentException("membership is required");
        if (subModule == null) throw new IllegalArgumentException("subModule is required");
        this.id = id;
        this.membership = membership;
        this.subModule = subModule;
        this.createdDate = createdDate;
    }

    public static MembershipSubModule create(MembershipRef membership, SubModuleRef subModule) {
        return new MembershipSubModule(null, membership, subModule, LocalDateTime.now());
    }

    public void update(MembershipRef membership, SubModuleRef subModule) {
        if (membership == null) throw new IllegalArgumentException("membership is required");
        if (subModule == null) throw new IllegalArgumentException("subModule is required");
        this.membership = membership;
        this.subModule = subModule;
    }

    public Long getId() { return id; }
    public MembershipRef getMembership() { return membership; }
    public SubModuleRef getSubModule() { return subModule; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
