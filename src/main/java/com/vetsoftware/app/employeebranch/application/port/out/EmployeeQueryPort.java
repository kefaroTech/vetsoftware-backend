package com.vetsoftware.app.employeebranch.application.port.out;

public interface EmployeeQueryPort {

    /** ¿El empleado existe (activo) y pertenece a la empresa? Valida ownership antes de reasignar sedes. */
    boolean existsByIdAndCompanyId(Long employeeId, Long companyId);
}
