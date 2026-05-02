package com.vetsoftware.app.employee.domain;

import java.time.LocalDateTime;

public class Employee {
    private Long id;
    private String employeeCode;
    private String hashPassword;
    private String name;
    private String email;
    private EmployeeStatus status;
    private final CompanyRef company;
    private final LocalDateTime createdDate;

    public Employee(Long id, String employeeCode, String hashPassword, String name, String email,
                    EmployeeStatus status, CompanyRef company, LocalDateTime createdDate) {
        if (employeeCode == null || employeeCode.isBlank()) throw new IllegalArgumentException("employeeCode is required");
        if (employeeCode.length() > 50) throw new IllegalArgumentException("employeeCode must be 50 chars or less");
        if (hashPassword == null || hashPassword.isBlank()) throw new IllegalArgumentException("password is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        if (email.length() > 100) throw new IllegalArgumentException("email must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (company == null) throw new IllegalArgumentException("company is required");
        this.id = id;
        this.employeeCode = employeeCode;
        this.hashPassword = hashPassword;
        this.name = name;
        this.email = email;
        this.status = status;
        this.company = company;
        this.createdDate = createdDate;
    }

    public static Employee create(String employeeCode, String hashPassword, String name, String email,
                                  EmployeeStatus status, CompanyRef company) {
        return new Employee(null, employeeCode, hashPassword, name, email,
            status, company, LocalDateTime.now());
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
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
