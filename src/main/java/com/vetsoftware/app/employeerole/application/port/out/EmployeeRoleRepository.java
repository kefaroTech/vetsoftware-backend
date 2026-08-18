package com.vetsoftware.app.employeerole.application.port.out;

import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import java.util.List;
import java.util.Optional;

public interface EmployeeRoleRepository {
    EmployeeRole save(EmployeeRole employeeRole);

    Optional<EmployeeRole> findById(Long id);

    /**
     * Lectura acotada al tenant. La empresa de una asignacion es la del empleado
     * titular: la fila no tiene {@code companyId} propio.
     */
    Optional<EmployeeRole> findByIdAndCompanyId(Long id, Long companyId);

    List<EmployeeRole> findAll();

    void delete(Long id);

    int reactivate(Long id);

    /**
     * Reactivacion acotada al tenant; devuelve las filas afectadas. Cero significa
     * «no existe en esa empresa», que es tambien la respuesta correcta para la
     * asignacion de otro tenant.
     */
    int reactivate(Long id, Long companyId);

    Optional<Long> findDisabledIdByEmployeeAndRole(Long employeeId, Long roleId);
}
