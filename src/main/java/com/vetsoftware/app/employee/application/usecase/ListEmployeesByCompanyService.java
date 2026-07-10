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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public List<EmployeeDto> listByCompany(Long companyId) {
        // Incluye desactivados: la pantalla muestra el estado (Activo/Inactivo/Invitado).
        List<Employee> employees = repository.findAllByCompanyIdIncludingDisabled(companyId);
        List<Long> ids = employees.stream().map(Employee::getId).toList();
        // Para el listado: los desactivados muestran el rol que tenían (asignaciones aunque deshabilitadas).
        Map<Long, List<RoleSnapshot>> rolesByEmployee = rolesQueryPort.findRolesForListing(ids);
        return employees.stream()
            .map(e -> EmployeeDto.from(e, rolesByEmployee.getOrDefault(e.getId(), List.of())))
            .toList();
    }
}
