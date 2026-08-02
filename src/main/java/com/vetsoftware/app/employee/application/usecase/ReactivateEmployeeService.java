package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.ReactivateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.reactivate")
@Service
public class ReactivateEmployeeService implements ReactivateEmployeeUseCase {
  private final EmployeeRepository repository;

  public ReactivateEmployeeService(EmployeeRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public EmployeeDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new EmployeeNotFoundException(id);
    return EmployeeDto.from(
        repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id)));
  }
}
