package com.vetsoftware.app.generalchargeopenaccount.domain;

public record EmployeeRef(Long id, String name) {
    public EmployeeRef {
        if (id == null)
            throw new IllegalArgumentException("employee id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("employee name is required");
    }
}
