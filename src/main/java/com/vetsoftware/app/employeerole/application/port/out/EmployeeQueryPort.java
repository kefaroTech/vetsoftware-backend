package com.vetsoftware.app.employeerole.application.port.out;

import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import java.util.Optional;

public interface EmployeeQueryPort {
    Optional<EmployeeRef> findById(Long employeeId);

    /**
     * Resolucion acotada al tenant de la referencia entrante. Un empleado no es un
     * catalogo global: pertenece a una empresa, y resolverlo por id a secas dejaba
     * que un administrador de A le asignara un rol a un empleado de B. Vacio
     * significa «no existe en TU empresa», que es tambien la respuesta correcta
     * para el empleado de otro tenant.
     */
    Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId);
}
