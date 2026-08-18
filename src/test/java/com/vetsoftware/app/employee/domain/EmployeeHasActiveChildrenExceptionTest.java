package com.vetsoftware.app.employee.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Excepción de dominio declarada y registrada en
 * {@code GlobalExceptionHandler}, pero — a diferencia de su análoga en
 * {@code role} — hoy sin ningún caller: ningún caso de uso de esta feature la
 * lanza (ver informe de la campaña de cobertura). Se prueba igual porque es la
 * parte estable de su contrato (el mensaje), no un getter.
 */
class EmployeeHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje identifica el empleado y el tipo de hijo activo")
    void el_mensaje_identifica_el_empleado_y_el_tipo_de_hijo() {
        EmployeeHasActiveChildrenException exception = new EmployeeHasActiveChildrenException(55L,
                "employee_roles");

        assertThat(exception.getMessage()).contains("55").contains("employee_roles");
    }
}
