package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.employee.application.port.in.SearchEmployeesUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeBranchesQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.BranchRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.search.by.company")
@Service
public class SearchEmployeesByCompanyService implements SearchEmployeesUseCase {
    private final EmployeeRepository repository;
    private final EmployeeRolesQueryPort rolesQueryPort;
    private final EmployeeBranchesQueryPort branchesQueryPort;

    public SearchEmployeesByCompanyService(EmployeeRepository repository,
            EmployeeRolesQueryPort rolesQueryPort, EmployeeBranchesQueryPort branchesQueryPort) {
        this.repository = repository;
        this.rolesQueryPort = rolesQueryPort;
        this.branchesQueryPort = branchesQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EmployeeDto> search(SearchEmployeesCommand command) {
        // Solo se cargan roles y sedes de la página actual (batch por ids), evitando
        // N+1.
        PageResult<Employee> page = repository.search(command);
        List<Long> ids = page.content().stream().map(Employee::getId).toList();
        // Los desactivados muestran el rol que tenían (asignaciones aunque
        // deshabilitadas).
        Map<Long, List<RoleSnapshot>> rolesByEmployee = rolesQueryPort.findRolesForListing(ids);
        Map<Long, List<BranchRef>> branchesByEmployee = branchesQueryPort
                .findBranchesByEmployeeIds(ids);
        return page.map(e -> EmployeeDto.from(e, rolesByEmployee.getOrDefault(e.getId(), List.of()),
                branchesByEmployee.getOrDefault(e.getId(), List.of())));
    }
}
