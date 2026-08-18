package com.vetsoftware.app.employee.application.port.out;

import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.employee.domain.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Employee save(Employee employee);

    Optional<Employee> findById(Long id);

    Optional<Employee> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Busca por id incluyendo empleados desactivados (para operaciones idempotentes
     * como desactivar). Sin acotar: solo el camino SYSTEM
     * ({@code companyId == null}).
     */
    Optional<Employee> findByIdIncludingDisabled(Long id);

    /**
     * Igual que {@link #findByIdIncludingDisabled} pero acotada a la empresa: el
     * empleado de otro tenant es un 404, no una fila que se pueda desactivar.
     */
    Optional<Employee> findByIdIncludingDisabledAndCompanyId(Long id, Long companyId);

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

    /**
     * Baja logica acotada a la empresa. {@code companyId} null es el camino SYSTEM,
     * cross-tenant por diseno; con empresa, el UPDATE lleva su
     * {@code AND company_id} y la fila ajena no se toca.
     */
    void delete(Long id, Long companyId);

    int reactivate(Long id);

    /**
     * Reactivacion acotada al tenant; devuelve las filas afectadas. Cero significa
     * «no existe en esa empresa», que es tambien la respuesta correcta para el
     * empleado de otro tenant.
     */
    int reactivate(Long id, Long companyId);

    /**
     * ¿Existe ya ese código de empleado? Cuenta TODAS las filas (incluidas las
     * desactivadas), acorde a la constraint unique de la BD, para validar
     * disponibilidad al autogenerar/editar el código.
     */
    boolean codeExists(String employeeCode);
}
