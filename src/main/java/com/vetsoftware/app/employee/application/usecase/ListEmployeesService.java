package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "employee.list")
@Service
public class ListEmployeesService implements ListEmployeesUseCase {
  private final EmployeeRepository repository;

  public ListEmployeesService(EmployeeRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<EmployeeDto> listAll() {
    return repository.findAll().stream().map(EmployeeDto::from).toList();
  }
}
