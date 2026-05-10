package com.vetsoftware.app.employee.application.port.out;

import com.vetsoftware.app.employee.domain.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(Long id);
    List<Employee> findAll();
    List<Employee> findAllByCompanyId(Long companyId);
    void delete(Long id);
}
