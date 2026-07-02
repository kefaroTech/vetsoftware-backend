package com.vetsoftware.app.employee.domain;

import java.time.LocalDateTime;

public class Employee {
    private Long id;
    private String employeeCode;
    private String hashPassword;
    private String name;
    private String email;
    private final CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private Long authVersion;

    public Employee(Long id, String employeeCode, String hashPassword, String name, String email,
                    CompanyRef company, LocalDateTime createdDate, boolean enabled, Long authVersion) {
        if (employeeCode == null || employeeCode.isBlank()) throw new IllegalArgumentException("employeeCode is required");
        if (employeeCode.length() > 50) throw new IllegalArgumentException("employeeCode must be 50 chars or less");
        if (hashPassword == null || hashPassword.isBlank()) throw new IllegalArgumentException("password is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        if (email.length() > 100) throw new IllegalArgumentException("email must be 100 chars or less");
        if (company == null) throw new IllegalArgumentException("company is required");
        this.id = id;
        this.employeeCode = employeeCode;
        this.hashPassword = hashPassword;
        this.name = name;
        this.email = email;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.authVersion = authVersion == null ? 0L : authVersion;
    }

    public static Employee create(String employeeCode, String hashPassword, String name, String email,
                                  CompanyRef company) {
        return new Employee(null, employeeCode, hashPassword, name, email,
            company, LocalDateTime.now(), true, 0L);
    }

    public void update(String employeeCode, String name, String email) {
        if (employeeCode == null || employeeCode.isBlank()) throw new IllegalArgumentException("employeeCode is required");
        if (employeeCode.length() > 50) throw new IllegalArgumentException("employeeCode must be 50 chars or less");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        if (email.length() > 100) throw new IllegalArgumentException("email must be 100 chars or less");
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
    }

    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getHashPassword() { return hashPassword; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Long getAuthVersion() { return authVersion; }
}
