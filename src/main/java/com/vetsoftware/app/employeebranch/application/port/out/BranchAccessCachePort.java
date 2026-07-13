package com.vetsoftware.app.employeebranch.application.port.out;

public interface BranchAccessCachePort {

    /** Invalida el cache de sedes del empleado (employee-branch-ids) tras reasignar. */
    void evictByEmployeeId(Long employeeId);
}
