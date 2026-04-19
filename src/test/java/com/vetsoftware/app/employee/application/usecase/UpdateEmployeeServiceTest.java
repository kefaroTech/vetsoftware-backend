package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpdateEmployeeServiceTest {
    private Employee preset = new Employee(1L, "OLD-001", "secret", "Old Name", "old@vet.com", EmployeeStatus.ACTIVE, 1L, LocalDateTime.now(), null);
    private final EmployeeRepository repository = new EmployeeRepository() {
        @Override public Employee save(Employee e) { return e; }
        @Override public Optional<Employee> findById(Long id) { return Optional.ofNullable(preset); }
        @Override public List<Employee> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final UpdateEmployeeService service = new UpdateEmployeeService(repository);

    @Test
    void update_employee_changes_fields_and_returns_dto() {
        EmployeeDto dto = service.execute(new UpdateEmployeeCommand(1L, "NEW-001", "New Name", "new@vet.com", "INACTIVE"));
        assertEquals("NEW-001", dto.employeeCode());
        assertEquals("New Name", dto.name());
        assertEquals("INACTIVE", dto.status());
    }

    @Test
    void update_employee_not_found_throws() {
        preset = null;
        assertThrows(EmployeeNotFoundException.class, () ->
            service.execute(new UpdateEmployeeCommand(99L, "EMP-001", "Name", "email@vet.com", "ACTIVE")));
    }
}
