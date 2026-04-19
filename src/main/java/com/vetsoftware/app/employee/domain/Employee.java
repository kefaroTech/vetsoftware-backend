package com.vetsoftware.app.employee.domain;

import java.time.LocalDateTime;

public class Employee {
    private Long id;
    private String employeeCode;
    private String hashPassword;
    private String name;
    private String email;
    private EmployeeStatus status;
    private final Long companyId;
    private final LocalDateTime createdDate;
    private final Long createdBy;

    public Employee(Long id, String employeeCode, String hashPassword, String name, String email,
                    EmployeeStatus status, Long companyId, LocalDateTime createdDate, Long createdBy) {
        if (employeeCode == null || employeeCode.isBlank()) throw new IllegalArgumentException("employeeCode is required");
        if (employeeCode.length() > 50) throw new IllegalArgumentException("employeeCode must be 50 chars or less");
        if (hashPassword == null || hashPassword.isBlank()) throw new IllegalArgumentException("password is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        if (email.length() > 100) throw new IllegalArgumentException("email must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        this.id = id;
        this.employeeCode = employeeCode;
        this.hashPassword = hashPassword;
        this.name = name;
        this.email = email;
        this.status = status;
        this.companyId = companyId;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
    }

    public static Employee create(String employeeCode, String hashPassword, String name, String email,
                                  EmployeeStatus status, Long companyId, Long createdBy) {
        return new Employee(null, employeeCode, hashPassword, name, email,
            status, companyId, LocalDateTime.now(), createdBy);
    }

    public void update(String employeeCode, String name, String email, EmployeeStatus status) {
        if (employeeCode == null || employeeCode.isBlank()) throw new IllegalArgumentException("employeeCode is required");
        if (employeeCode.length() > 50) throw new IllegalArgumentException("employeeCode must be 50 chars or less");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        if (email.length() > 100) throw new IllegalArgumentException("email must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getHashPassword() { return hashPassword; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public EmployeeStatus getStatus() { return status; }
    public Long getCompanyId() { return companyId; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Long getCreatedBy() { return createdBy; }
}
