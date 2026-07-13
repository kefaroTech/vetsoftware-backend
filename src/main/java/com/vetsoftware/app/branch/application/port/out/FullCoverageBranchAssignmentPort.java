package com.vetsoftware.app.branch.application.port.out;

public interface FullCoverageBranchAssignmentPort {

    /**
     * Al crear una sede nueva, asígnala a los empleados de la empresa que tienen "todas las sedes" (cobertura total
     * de las demás). Así quien operaba en toda la empresa hereda automáticamente la sede nueva; quien estaba limitado
     * a un subconjunto estricto, no. Idempotente (no re-inserta si el empleado ya la tiene).
     */
    void assignNewBranchToFullCoverageEmployees(Long companyId, Long newBranchId);
}
