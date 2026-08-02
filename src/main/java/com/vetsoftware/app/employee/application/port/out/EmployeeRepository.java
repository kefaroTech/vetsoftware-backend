package com.vetsoftware.app.employee.application.port.out;

import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.employee.application.dto.PageResult;
import com.vetsoftware.app.employee.domain.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Employee save(Employee employee);

    Optional<Employee> findById(Long id);

    Optional<Employee> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Busca por id incluyendo empleados desactivados (para operaciones idempotentes
     * como desactivar).
     */
    Optional<Employee> findByIdIncludingDisabled(Long id);

    List<Employee> findAll();

    List<Employee> findAllByCompanyId(Long companyId);

    /**
     * Empleados de la company incluyendo los desactivados (para la pantalla de
     * listado).
     */
    List<Employee> findAllByCompanyIdIncludingDisabled(Long companyId);

    /**
     * Búsqueda paginada por empresa (nombre/código/correo), incluyendo
     * desactivados.
     */
    PageResult<Employee> search(SearchEmployeesCommand command);

    void delete(Long id);

    int reactivate(Long id);

    /**
     * ¿Existe ya ese código de empleado? Cuenta TODAS las filas (incluidas las
     * desactivadas), acorde a la constraint unique de la BD, para validar
     * disponibilidad al autogenerar/editar el código.
     */
    boolean codeExists(String employeeCode);
}
