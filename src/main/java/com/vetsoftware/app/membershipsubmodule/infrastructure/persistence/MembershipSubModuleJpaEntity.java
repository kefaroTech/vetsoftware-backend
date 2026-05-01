package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_sub_modules")
public class MembershipSubModuleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "membership_id", nullable = false)
    private MembershipJpaEntity membership;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_module_id", nullable = false)
    private SubModuleJpaEntity subModule;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected MembershipSubModuleJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MembershipJpaEntity getMembership() { return membership; }
    public void setMembership(MembershipJpaEntity membership) { this.membership = membership; }
    public SubModuleJpaEntity getSubModule() { return subModule; }
    public void setSubModule(SubModuleJpaEntity subModule) { this.subModule = subModule; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
