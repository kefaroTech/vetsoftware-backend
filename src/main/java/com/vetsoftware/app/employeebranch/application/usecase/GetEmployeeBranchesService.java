package com.vetsoftware.app.employeebranch.application.usecase;

import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import com.vetsoftware.app.employeebranch.application.port.in.GetEmployeeBranchesUseCase;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeBranchRepository;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeQueryPort;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "employee.branch.get")
@Service
public class GetEmployeeBranchesService implements GetEmployeeBranchesUseCase {
  private final EmployeeBranchRepository repository;
  private final EmployeeQueryPort employeeQueryPort;

  public GetEmployeeBranchesService(
      EmployeeBranchRepository repository, EmployeeQueryPort employeeQueryPort) {
    this.repository = repository;
    this.employeeQueryPort = employeeQueryPort;
  }

  @Override
  public EmployeeBranchesDto execute(Long employeeId, Long companyId) {
    if (!employeeQueryPort.existsByIdAndCompanyId(employeeId, companyId)) {
      throw new IllegalArgumentException("Employee not found: " + employeeId);
    }
    return new EmployeeBranchesDto(employeeId, repository.findBranchIdsByEmployeeId(employeeId));
  }
}
