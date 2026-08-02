package com.vetsoftware.app.employeerole.domain;

import java.time.LocalDateTime;

public class EmployeeRole {
    private Long id;
    private EmployeeRef employee;
    private RoleRef role;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public EmployeeRole(Long id, EmployeeRef employee, RoleRef role, LocalDateTime createdDate,
            boolean enabled) {
        if (employee == null)
            throw new IllegalArgumentException("employee is required");
        if (role == null)
            throw new IllegalArgumentException("role is required");
        this.id = id;
        this.employee = employee;
        this.role = role;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static EmployeeRole create(EmployeeRef employee, RoleRef role) {
        return new EmployeeRole(null, employee, role, LocalDateTime.now(), true);
    }

    public void update(EmployeeRef employee, RoleRef role) {
        if (employee == null)
            throw new IllegalArgumentException("employee is required");
        if (role == null)
            throw new IllegalArgumentException("role is required");
        this.employee = employee;
        this.role = role;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public EmployeeRef getEmployee() {
        return employee;
    }

    public RoleRef getRole() {
        return role;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
