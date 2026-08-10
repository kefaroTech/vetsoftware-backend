package com.vetsoftware.app.employeerole.testsupport;

import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.command.UpdateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.RoleRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo employeerole.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code EmployeeRole.create(...)}: el factory pone {@code LocalDateTime.now()}
 * y haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class EmployeeRoleMother {

    public static final Long EMPLOYEE_ROLE_ID = 500L;

    public static final EmployeeRef EMPLEADO = new EmployeeRef(7L, "EMP-007", "Ana Ruiz");
    public static final EmployeeRef OTRO_EMPLEADO = new EmployeeRef(8L, "EMP-008", "Luis Paz");

    public static final RoleRef ROL_VETERINARIO = new RoleRef(3L, "Veterinario", "VET");
    public static final RoleRef ROL_RECEPCION = new RoleRef(4L, "Recepcion", "REC");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private EmployeeRoleMother() {
    }

    /** Asignacion habilitada del empleado 7 con el rol 3. El caso por defecto. */
    public static EmployeeRole habilitado() {
        return habilitado(EMPLOYEE_ROLE_ID);
    }

    public static EmployeeRole habilitado(Long id) {
        return new EmployeeRole(id, EMPLEADO, ROL_VETERINARIO, CREADO, true);
    }

    /** Misma asignacion pero con el borrado logico ya aplicado. */
    public static EmployeeRole deshabilitado() {
        return new EmployeeRole(EMPLOYEE_ROLE_ID, EMPLEADO, ROL_VETERINARIO, CREADO, false);
    }

    /** Asignacion de otro empleado, para los escenarios de cambio de titular. */
    public static EmployeeRole deOtroEmpleado() {
        return new EmployeeRole(EMPLOYEE_ROLE_ID, OTRO_EMPLEADO, ROL_RECEPCION, CREADO, true);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateEmployeeRoleCommand comandoCrear() {
        return new CreateEmployeeRoleCommand(EMPLEADO.id(), ROL_VETERINARIO.id());
    }

    /** Comando que reasigna la fila al otro empleado y al otro rol. */
    public static UpdateEmployeeRoleCommand comandoActualizar() {
        return new UpdateEmployeeRoleCommand(EMPLOYEE_ROLE_ID, OTRO_EMPLEADO.id(),
                ROL_RECEPCION.id());
    }

    /** Comando que conserva el empleado y solo cambia el rol. */
    public static UpdateEmployeeRoleCommand comandoActualizarMismoEmpleado() {
        return new UpdateEmployeeRoleCommand(EMPLOYEE_ROLE_ID, EMPLEADO.id(), ROL_RECEPCION.id());
    }
}
