package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListEmployeesServiceTest {
    private final List<Employee> employees = List.of(
        new Employee(1L, "EMP-001", "secret", "Alice", "alice@vet.com", EmployeeStatus.ACTIVE,   1L, LocalDateTime.now(), null),
        new Employee(2L, "EMP-002", "secret", "Bob",   "bob@vet.com",   EmployeeStatus.INACTIVE, 1L, LocalDateTime.now(), null)
    );
    private final EmployeeRepository repository = new EmployeeRepository() {
        @Override public Employee save(Employee e) { return e; }
        @Override public Optional<Employee> findById(Long id) { return Optional.empty(); }
        @Override public List<Employee> findAll() { return employees; }
        @Override public void delete(Long id) {}
    };
    private final ListEmployeesService service = new ListEmployeesService(repository);

    @Test
    void list_all_returns_all_employees() {
        List<EmployeeDto> result = service.listAll();
        assertEquals(2, result.size());
        assertEquals("EMP-001", result.get(0).employeeCode());
        assertEquals("EMP-002", result.get(1).employeeCode());
    }

    @Test
    void list_all_empty_returns_empty_list() {
        EmployeeRepository emptyRepo = new EmployeeRepository() {
            @Override public Employee save(Employee e) { return e; }
            @Override public Optional<Employee> findById(Long id) { return Optional.empty(); }
            @Override public List<Employee> findAll() { return List.of(); }
            @Override public void delete(Long id) {}
        };
        assertTrue(new ListEmployeesService(emptyRepo).listAll().isEmpty());
    }
}
