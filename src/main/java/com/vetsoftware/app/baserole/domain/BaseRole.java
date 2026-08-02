package com.vetsoftware.app.baserole.domain;

import java.time.LocalDateTime;

public class BaseRole {
    private Long id;
    private String name;
    private String code;
    private Boolean mandatory;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public BaseRole(Long id, String name, String code, Boolean mandatory, LocalDateTime createdDate,
            boolean enabled) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 100)
            throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("code must be 50 chars or less");
        if (mandatory == null)
            throw new IllegalArgumentException("mandatory is required");
        this.id = id;
        this.name = name;
        this.code = code;
        this.mandatory = mandatory;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static BaseRole create(String name, String code, Boolean mandatory) {
        return new BaseRole(null, name, code, mandatory, LocalDateTime.now(), true);
    }

    public void update(String name, String code, Boolean mandatory) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 100)
            throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("code must be 50 chars or less");
        if (mandatory == null)
            throw new IllegalArgumentException("mandatory is required");
        this.name = name;
        this.code = code;
        this.mandatory = mandatory;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
