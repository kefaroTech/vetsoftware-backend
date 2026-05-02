package com.vetsoftware.app.permission.domain;

import java.time.LocalDateTime;

public class Permission {
    private Long id;
    private String name;
    private String code;
    private CompanyRef company;
    private SubModuleRef subModule;
    private final LocalDateTime createdDate;

    public Permission(Long id, String name, String code, CompanyRef company, SubModuleRef subModule, LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (company == null) throw new IllegalArgumentException("company is required");
        if (subModule == null) throw new IllegalArgumentException("subModule is required");
        this.id = id;
        this.name = name;
        this.code = code;
        this.company = company;
        this.subModule = subModule;
        this.createdDate = createdDate;
    }

    public static Permission create(String name, String code, CompanyRef company, SubModuleRef subModule) {
        return new Permission(null, name, code, company, subModule, LocalDateTime.now());
    }

    public void update(String name, String code, CompanyRef company, SubModuleRef subModule) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (company == null) throw new IllegalArgumentException("company is required");
        if (subModule == null) throw new IllegalArgumentException("subModule is required");
        this.name = name;
        this.code = code;
        this.company = company;
        this.subModule = subModule;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public CompanyRef getCompany() { return company; }
    public SubModuleRef getSubModule() { return subModule; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
