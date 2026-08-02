package com.vetsoftware.app.employee.application.port.out;

import java.util.List;

/**
 * Asigna sedes a un empleado durante el alta (delegado a la feature
 * employeebranch).
 */
public interface EmployeeBranchAssigner {
    void assign(Long employeeId, Long companyId, List<Long> branchIds);
}
