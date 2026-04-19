package com.vetsoftware.app.domain;

public record EmployeeId(Long value) {
    public static EmployeeId of(Long value) {
        return new EmployeeId(value);
    }
}
