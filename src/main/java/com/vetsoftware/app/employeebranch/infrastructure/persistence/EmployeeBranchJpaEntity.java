package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

/**
 * Asignación de una sede ({@link BranchJpaEntity}) a un empleado ({@link EmployeeJpaEntity}). Única fuente del alcance
 * por sede: "todas las sedes" se materializa como una fila por sede; cero filas = sin acceso a recursos scopeados a
 * sede. Soft-delete por {@code enabled}, igual que {@code employee_roles}.
 */
@Entity
@Table(name = "employee_branches", uniqueConstraints = {
    @UniqueConstraint(name = "uq_employee_branches", columnNames = {"employee_id", "branch_id"})
})
@SQLDelete(sql = "UPDATE employee_branches SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class EmployeeBranchJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeJpaEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchJpaEntity branch;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public EmployeeBranchJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EmployeeJpaEntity getEmployee() { return employee; }
    public void setEmployee(EmployeeJpaEntity employee) { this.employee = employee; }
    public BranchJpaEntity getBranch() { return branch; }
    public void setBranch(BranchJpaEntity branch) { this.branch = branch; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
