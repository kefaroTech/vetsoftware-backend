package com.vetsoftware.app.employeerole.application.port.out;

import com.vetsoftware.app.employeerole.domain.RoleRef;
import java.util.Optional;

public interface RoleQueryPort {
    Optional<RoleRef> findById(Long roleId);

    /**
     * Resolucion acotada al tenant de la referencia entrante. Los roles tienen
     * {@code company_id} —el unique es {@code (company_id, code)}—, asi que el rol
     * tambien es de alguien: colgar el rol de B de un empleado de A le entrega los
     * permisos que la membresia de A no autoriza. No basta con acotar el empleado.
     */
    Optional<RoleRef> findByIdAndCompanyId(Long roleId, Long companyId);
}
