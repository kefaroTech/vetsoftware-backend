package com.vetsoftware.app.employeerole.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_roles", uniqueConstraints = {
    @UniqueConstraint(name = "uq_employee_roles", columnNames = {"employee_id", "role_id"})
})
@SQLDelete(sql = "UPDATE employee_roles SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class EmployeeRoleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeJpaEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public EmployeeRoleJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EmployeeJpaEntity getEmployee() { return employee; }
    public void setEmployee(EmployeeJpaEntity employee) { this.employee = employee; }
    public RoleJpaEntity getRole() { return role; }
    public void setRole(RoleJpaEntity role) { this.role = role; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
