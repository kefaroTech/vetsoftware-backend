package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.dto.EmployeeDto;
import com.vetsoftware.app.application.port.out.EmployeeRepository;
import com.vetsoftware.app.domain.Employee;
import com.vetsoftware.app.domain.EmployeeNotFoundException;
import com.vetsoftware.app.domain.EmployeeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FindEmployeeServiceTest {
    private Employee stored = new Employee(1L, "EMP-001", "secret", "John Doe", "john@vet.com", EmployeeStatus.ACTIVE, 1L, LocalDateTime.now(), null);
    private final EmployeeRepository repository = new EmployeeRepository() {
        @Override public Employee save(Employee e) { return e; }
        @Override public Optional<Employee> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Employee> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final FindEmployeeService service = new FindEmployeeService(repository);

    @Test
    void find_existing_employee_returns_dto() {
        EmployeeDto dto = service.findById(1L);
        assertEquals("EMP-001", dto.employeeCode());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    void find_non_existing_employee_throws() {
        stored = null;
        assertThrows(EmployeeNotFoundException.class, () -> service.findById(99L));
    }
}
