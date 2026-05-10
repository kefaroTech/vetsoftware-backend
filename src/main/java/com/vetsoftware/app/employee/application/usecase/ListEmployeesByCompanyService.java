package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesByCompanyUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Observed(name = "employee.list_by_company")
@Service
public class ListEmployeesByCompanyService implements ListEmployeesByCompanyUseCase {
    private final EmployeeRepository repository;
    private final EmployeeRolesQueryPort rolesQueryPort;

    public ListEmployeesByCompanyService(EmployeeRepository repository,
                                          EmployeeRolesQueryPort rolesQueryPort) {
        this.repository = repository;
        this.rolesQueryPort = rolesQueryPort;
    }

    @Override
    public List<EmployeeDto> listByCompany(Long companyId) {
        List<Employee> employees = repository.findAllByCompanyId(companyId);
        List<Long> ids = employees.stream().map(Employee::getId).toList();
        Map<Long, List<RoleSnapshot>> rolesByEmployee = rolesQueryPort.findRolesByEmployeeIds(ids);
        return employees.stream()
            .map(e -> EmployeeDto.from(e, rolesByEmployee.getOrDefault(e.getId(), List.of())))
            .toList();
    }
}
