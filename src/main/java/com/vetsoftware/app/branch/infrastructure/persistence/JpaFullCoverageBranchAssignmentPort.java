package com.vetsoftware.app.branch.infrastructure.persistence;

import com.vetsoftware.app.branch.application.port.out.FullCoverageBranchAssignmentPort;
import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter del auto-registro por sede. Cruce permitido de vertical slicing:
 * infrastructure/persistence de {@code branch} usa el repo JPA de {@code employeebranch} para
 * materializar filas. La propagación a las sesiones vivas de los empleados afectados es por TTL del
 * cache {@code employee-branch-ids} (≤5 min); no se evicta explícitamente porque crear una sede es
 * una acción administrativa poco frecuente y de efecto no inmediato.
 */
@Component
public class JpaFullCoverageBranchAssignmentPort implements FullCoverageBranchAssignmentPort {

  private final EmployeeBranchJpaRepository employeeBranchJpaRepository;

  public JpaFullCoverageBranchAssignmentPort(
      EmployeeBranchJpaRepository employeeBranchJpaRepository) {
    this.employeeBranchJpaRepository = employeeBranchJpaRepository;
  }

  @Override
  public void assignNewBranchToFullCoverageEmployees(Long companyId, Long newBranchId) {
    List<Long> employeeIds =
        employeeBranchJpaRepository.findFullCoverageEmployeeIds(companyId, newBranchId);
    for (Long employeeId : employeeIds) {
      employeeBranchJpaRepository.insert(employeeId, newBranchId);
    }
  }
}
