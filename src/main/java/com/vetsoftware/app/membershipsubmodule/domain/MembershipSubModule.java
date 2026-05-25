package com.vetsoftware.app.membershipsubmodule.domain;

import java.time.LocalDateTime;

public class MembershipSubModule {
    private Long id;
    private MembershipRef membership;
    private SubModuleRef subModule;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public MembershipSubModule(Long id, MembershipRef membership, SubModuleRef subModule, LocalDateTime createdDate, boolean enabled) {
        if (membership == null) throw new IllegalArgumentException("membership is required");
        if (subModule == null) throw new IllegalArgumentException("subModule is required");
        this.id = id;
        this.membership = membership;
        this.subModule = subModule;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static MembershipSubModule create(MembershipRef membership, SubModuleRef subModule) {
        return new MembershipSubModule(null, membership, subModule, LocalDateTime.now(), true);
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
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
