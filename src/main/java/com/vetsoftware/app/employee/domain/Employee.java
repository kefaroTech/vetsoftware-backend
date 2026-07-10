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
    private boolean emailVerified;
    private boolean mustChangePassword;
    private Long authVersion;

    public Employee(Long id, String employeeCode, String hashPassword, String name, String email,
                    CompanyRef company, LocalDateTime createdDate, boolean enabled, boolean emailVerified,
                    boolean mustChangePassword, Long authVersion) {
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
        this.emailVerified = emailVerified;
        this.mustChangePassword = mustChangePassword;
        this.authVersion = authVersion == null ? 0L : authVersion;
    }

    /**
     * Crea un empleado nuevo. {@code emailVerified=false} para el dueño por auto-registro (Opción B, debe
     * verificar el correo antes de entrar). {@code mustChangePassword=true} para el staff invitado por un
     * admin (se le asigna una contraseña temporal y debe cambiarla en el primer login).
     */
    public static Employee create(String employeeCode, String hashPassword, String name, String email,
                                  CompanyRef company, boolean emailVerified, boolean mustChangePassword) {
        return new Employee(null, employeeCode, hashPassword, name, email,
            company, LocalDateTime.now(), true, emailVerified, mustChangePassword, 0L);
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

    public boolean isEmailVerified() { return emailVerified; }
    /** Marca el correo como verificado. Idempotente: verificar dos veces no tiene efecto adicional. */
    public void verifyEmail() { this.emailVerified = true; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    /** Cambia la contraseña (hash ya calculado) y limpia la obligación de cambiarla en el primer login. */
    public void changePassword(String newHashPassword) {
        if (newHashPassword == null || newHashPassword.isBlank())
            throw new IllegalArgumentException("password is required");
        this.hashPassword = newHashPassword;
        this.mustChangePassword = false;
    }

    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getHashPassword() { return hashPassword; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Long getAuthVersion() { return authVersion; }
}
