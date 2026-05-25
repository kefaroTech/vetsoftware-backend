package com.vetsoftware.app.module.domain;

import java.time.LocalDateTime;

public class Module {
    private Long id;
    private String name;
    private String code;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Module(Long id, String name, String code, LocalDateTime createdDate, boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        this.id = id;
        this.name = name;
        this.code = code;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Module create(String name, String code) {
        return new Module(null, name, code, LocalDateTime.now(), true);
    }

    public void update(String name, String code) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        this.name = name;
        this.code = code;
    }

    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
