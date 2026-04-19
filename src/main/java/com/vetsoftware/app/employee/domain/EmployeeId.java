package com.vetsoftware.app.employee.domain;

public record EmployeeId(Long value) {
    public static EmployeeId of(Long value) {
        return new EmployeeId(value);
    }
}
