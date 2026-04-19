package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.domain.MembershipStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
public class MembershipJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by")
    private Long createdBy;

    protected MembershipJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
