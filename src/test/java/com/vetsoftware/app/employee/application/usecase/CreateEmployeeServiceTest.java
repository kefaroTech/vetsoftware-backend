package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateEmployeeServiceTest {
    private final AtomicLong sequence = new AtomicLong(1);
    private final EmployeeRepository repository = new EmployeeRepository() {
        @Override public Employee save(Employee e) { return new Employee(sequence.getAndIncrement(), e.getEmployeeCode(), e.getHashPassword(), e.getName(), e.getEmail(), e.getStatus(), e.getCompanyId(), e.getCreatedDate(), e.getCreatedBy()); }
        @Override public Optional<Employee> findById(Long id) { return Optional.empty(); }
        @Override public List<Employee> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final CreateEmployeeService service = new CreateEmployeeService(repository);

    @Test
    void create_employee_saves_and_returns_dto() {
        CreateEmployeeCommand command = new CreateEmployeeCommand("EMP-001", "secret123", "John Doe", "john@vet.com", "ACTIVE", 1L, null);
        EmployeeDto dto = service.execute(command);
        assertNotNull(dto.id());
        assertEquals("EMP-001", dto.employeeCode());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    void create_employee_with_blank_code_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new CreateEmployeeCommand("", "secret", "John", "john@vet.com", "ACTIVE", 1L, null)));
    }

    @Test
    void create_employee_with_invalid_status_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new CreateEmployeeCommand("EMP-001", "secret", "John", "john@vet.com", "INVALID", 1L, null)));
    }

    @Test
    void create_employee_with_blank_name_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new CreateEmployeeCommand("EMP-001", "secret", "", "john@vet.com", "ACTIVE", 1L, null)));
    }
}
