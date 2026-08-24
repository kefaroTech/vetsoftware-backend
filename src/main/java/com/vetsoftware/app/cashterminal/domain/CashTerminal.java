package com.vetsoftware.app.cashterminal.domain;

import java.time.LocalDateTime;

/** Terminal de caja independiente de su representacion de persistencia. */
public class CashTerminal {

    private final Long id;
    private final Long companyId;
    private final Long branchId;
    private String name;
    private String code;
    private boolean active;
    private final LocalDateTime createdAt;
    private final Long version;

    public CashTerminal(Long id, Long companyId, Long branchId, String name, String code,
            boolean active, LocalDateTime createdAt, Long version) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (branchId == null)
            throw new IllegalArgumentException("branchId is required");
        validateName(name);
        validateCode(code);
        if (createdAt == null)
            throw new IllegalArgumentException("createdAt is required");
        this.id = id;
        this.companyId = companyId;
        this.branchId = branchId;
        this.name = name;
        this.code = code;
        this.active = active;
        this.createdAt = createdAt;
        this.version = version;
    }

    public static CashTerminal create(Long companyId, Long branchId, String name, String code,
            LocalDateTime createdAt) {
        return new CashTerminal(null, companyId, branchId, normalizeName(name), normalizeCode(code),
                true, createdAt, null);
    }

    public void rename(String name, String code) {
        this.name = normalizeName(name);
        this.code = normalizeCode(code);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private static String normalizeName(String value) {
        validateName(value);
        return value.trim();
    }

    private static String normalizeCode(String value) {
        validateCode(value);
        return value.trim().toUpperCase();
    }

    private static void validateName(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (value.trim().length() > 120)
            throw new IllegalArgumentException("El nombre supera 120 caracteres");
    }

    private static void validateCode(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El código es obligatorio");
        if (value.trim().length() > 60)
            throw new IllegalArgumentException("El código supera 60 caracteres");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}
