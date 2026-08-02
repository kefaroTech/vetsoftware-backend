package com.vetsoftware.app.employee.application.port.out;

import com.vetsoftware.app.employee.domain.BranchRef;
import java.util.List;
import java.util.Map;

public interface EmployeeBranchesQueryPort {
  /** Sedes vigentes asignadas a cada empleado (para el listado/detalle). */
  Map<Long, List<BranchRef>> findBranchesByEmployeeIds(List<Long> employeeIds);
}
