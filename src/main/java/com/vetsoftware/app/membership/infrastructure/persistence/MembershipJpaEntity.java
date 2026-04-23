package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "memberships")
public class MembershipJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "membership_modules",
        joinColumns = @JoinColumn(name = "membership_id"),
        inverseJoinColumns = @JoinColumn(name = "module_id")
    )
    private List<ModuleJpaEntity> modules = new ArrayList<>();

    protected MembershipJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public List<ModuleJpaEntity> getModules() { return modules; }
    public void setModules(List<ModuleJpaEntity> modules) { this.modules = modules; }
}
