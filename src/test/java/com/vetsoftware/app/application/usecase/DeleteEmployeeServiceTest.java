package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.port.out.EmployeeRepository;
import com.vetsoftware.app.domain.Employee;
import com.vetsoftware.app.domain.EmployeeNotFoundException;
import com.vetsoftware.app.domain.EmployeeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeleteEmployeeServiceTest {
    private Employee stored = new Employee(1L, "EMP-001", "secret", "John", "john@vet.com", EmployeeStatus.ACTIVE, 1L, LocalDateTime.now(), null);
    private boolean deleted;
    private final EmployeeRepository repository = new EmployeeRepository() {
        @Override public Employee save(Employee e) { return e; }
        @Override public Optional<Employee> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Employee> findAll() { return List.of(); }
        @Override public void delete(Long id) { deleted = true; }
    };
    private final DeleteEmployeeService service = new DeleteEmployeeService(repository);

    @Test
    void delete_existing_employee_removes_it() {
        service.execute(1L);
        assertTrue(deleted);
    }

    @Test
    void delete_non_existing_employee_throws() {
        stored = null;
        assertThrows(EmployeeNotFoundException.class, () -> service.execute(99L));
        assertFalse(deleted);
    }
}
