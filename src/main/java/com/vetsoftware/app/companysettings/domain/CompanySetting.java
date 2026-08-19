package com.vetsoftware.app.companysettings.domain;

import java.time.LocalDateTime;

/**
 * Ajuste de configuración por empresa (clave-valor). Ej.:
 * {@code inventory.allow_negative_stock =
 * "true"}.
 */
public class CompanySetting {
    private Long id;
    private final Long companyId;
    private final String propertyName;
    private String value;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    public CompanySetting(Long id, Long companyId, String propertyName, String value,
            LocalDateTime createdDate, Long version, boolean enabled) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (propertyName == null || propertyName.isBlank())
            throw new IllegalArgumentException("propertyName is required");
        if (propertyName.length() > 100)
            throw new IllegalArgumentException("propertyName must be 100 chars or less");
        if (value == null)
            throw new IllegalArgumentException("value is required");
        if (value.length() > 255)
            throw new IllegalArgumentException("value must be 255 chars or less");
        this.id = id;
        this.companyId = companyId;
        this.propertyName = propertyName;
        this.value = value;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static CompanySetting create(Long companyId, String propertyName, String value) {
        return new CompanySetting(null, companyId, propertyName, value, LocalDateTime.now(), null,
                true);
    }

    public void updateValue(String value) {
        if (value == null)
            throw new IllegalArgumentException("value is required");
        if (value.length() > 255)
            throw new IllegalArgumentException("value must be 255 chars or less");
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getValue() {
        return value;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
