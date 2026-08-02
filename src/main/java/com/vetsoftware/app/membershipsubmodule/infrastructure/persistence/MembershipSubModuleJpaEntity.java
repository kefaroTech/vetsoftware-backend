package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "membership_sub_modules",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_membership_sub_modules",
          columnNames = {"membership_id", "sub_module_id"})
    })
@SQLDelete(sql = "UPDATE membership_sub_modules SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class MembershipSubModuleJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membership_id", nullable = false)
  private MembershipJpaEntity membership;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sub_module_id", nullable = false)
  private SubModuleJpaEntity subModule;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  protected MembershipSubModuleJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public MembershipJpaEntity getMembership() {
    return membership;
  }

  public void setMembership(MembershipJpaEntity membership) {
    this.membership = membership;
  }

  public SubModuleJpaEntity getSubModule() {
    return subModule;
  }

  public void setSubModule(SubModuleJpaEntity subModule) {
    this.subModule = subModule;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
