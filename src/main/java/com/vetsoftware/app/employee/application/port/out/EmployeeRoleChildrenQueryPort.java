package com.vetsoftware.app.employee.application.port.out;

public interface EmployeeRoleChildrenQueryPort {
    boolean existsActiveByEmployeeId(Long parentId);
}
