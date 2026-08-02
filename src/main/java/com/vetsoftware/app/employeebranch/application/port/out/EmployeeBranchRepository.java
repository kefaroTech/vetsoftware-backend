package com.vetsoftware.app.employeebranch.application.port.out;

import java.util.Collection;
import java.util.List;

public interface EmployeeBranchRepository {

    /** Ids de las sedes asignadas (vigentes) al empleado. */
    List<Long> findBranchIdsByEmployeeId(Long employeeId);

    /**
     * Deja al empleado con EXACTAMENTE estas sedes (desactiva las que sobran,
     * reactiva/inserta las que faltan).
     */
    void replaceBranches(Long employeeId, Collection<Long> branchIds);
}
