package com.vetsoftware.app.employee.infrastructure.orchestration;

import com.vetsoftware.app.employee.application.port.out.EmployeeBranchAssigner;
import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.port.in.SetEmployeeBranchesUseCase;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación: asigna las sedes del alta vía la feature employeebranch (set atómico).
 * Reusa su validación (sedes ∈ empresa, rechazo de set vacío) e invalidación de cache.
 */
@Component
public class EmployeeBranchAssignerAdapter implements EmployeeBranchAssigner {

  private final SetEmployeeBranchesUseCase setUseCase;

  public EmployeeBranchAssignerAdapter(SetEmployeeBranchesUseCase setUseCase) {
    this.setUseCase = setUseCase;
  }

  @Override
  public void assign(Long employeeId, Long companyId, List<Long> branchIds) {
    setUseCase.execute(new SetEmployeeBranchesCommand(employeeId, companyId, false, branchIds));
  }
}
