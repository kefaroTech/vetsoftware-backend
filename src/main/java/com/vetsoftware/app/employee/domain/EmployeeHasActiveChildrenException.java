package com.vetsoftware.app.employee.domain;

public class EmployeeHasActiveChildrenException extends RuntimeException {
    public EmployeeHasActiveChildrenException(Long id, String childType) {
        super("Cannot delete employee " + id + ": has active " + childType + " children");
    }
}
