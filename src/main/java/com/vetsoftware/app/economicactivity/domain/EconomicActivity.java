package com.vetsoftware.app.economicactivity.domain;

import java.time.LocalDateTime;

public class EconomicActivity {
    private Long id;
    private String code;
    private String name;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public EconomicActivity(Long id, String code, String name, LocalDateTime createdDate,
            boolean enabled) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 20)
            throw new IllegalArgumentException("code must be 20 chars or less");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 150)
            throw new IllegalArgumentException("name must be 150 chars or less");
        this.id = id;
        this.code = code;
        this.name = name;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static EconomicActivity create(String code, String name) {
        return new EconomicActivity(null, code, name, LocalDateTime.now(), true);
    }

    public void update(String code, String name) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 20)
            throw new IllegalArgumentException("code must be 20 chars or less");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 150)
            throw new IllegalArgumentException("name must be 150 chars or less");
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
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
