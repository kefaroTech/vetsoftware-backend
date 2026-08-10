package com.vetsoftware.app.employee.testsupport;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.domain.BranchRef;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo employee.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Employee.create(...)} porque el factory pone
 * {@code LocalDateTime.now()} y volveria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class EmployeeMother {

    public static final Long EMPLOYEE_ID = 55L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 77L;

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    public static final CompanyRef VETRINA = new CompanyRef(COMPANY_ID, "Veterinaria Vetrina",
            "900123456");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_COMPANY_ID, "Clinica Norte",
            "800987654");

    public static final RoleSnapshot VETERINARIO = new RoleSnapshot(500L, 3L, "Veterinario", "VET");
    public static final RoleSnapshot CAJERO = new RoleSnapshot(501L, 4L, "Cajero", "CASHIER");
    public static final RoleSnapshot ADMINISTRADOR = new RoleSnapshot(502L, 1L, "Administrador",
            "ADMIN");

    public static final BranchRef SEDE_NORTE = new BranchRef(7L, "Sede Norte");
    public static final BranchRef SEDE_SUR = new BranchRef(8L, "Sede Sur");

    public static final String HASH = "$2a$10$hash";

    private EmployeeMother() {
    }

    /** Empleado activo, habilitado, con correo verificado. El caso por defecto. */
    public static Employee activo() {
        return activo(EMPLOYEE_ID);
    }

    public static Employee activo(Long id) {
        return new Employee(id, "VV-MARIANA", HASH, "Mariana Rojas", "mariana@vetrina.co", VETRINA,
                CREADO, true, true, false, EmployeeStatus.ACTIVE, 0L);
    }

    /** Staff recien invitado: aun no inicia sesion y debe cambiar la clave. */
    public static Employee invitado() {
        return new Employee(EMPLOYEE_ID, "VV-MARIANA", HASH, "Mariana Rojas", "mariana@vetrina.co",
                VETRINA, CREADO, true, true, true, EmployeeStatus.INVITED, 0L);
    }

    /** Empleado desactivado (soft-delete) que conserva su historia. */
    public static Employee deshabilitado() {
        return new Employee(EMPLOYEE_ID, "VV-MARIANA", HASH, "Mariana Rojas", "mariana@vetrina.co",
                VETRINA, CREADO, false, true, false, EmployeeStatus.ACTIVE, 4L);
    }

    /** Empleado que pertenece a otro tenant: sirve para probar el aislamiento. */
    public static Employee deOtraEmpresa() {
        return new Employee(EMPLOYEE_ID, "CN-PEDRO", HASH, "Pedro Diaz", "pedro@norte.co",
                OTRA_CLINICA, CREADO, true, true, false, EmployeeStatus.ACTIVE, 0L);
    }

    public static CreateEmployeeCommand comandoCrear() {
        return comandoCrear(true);
    }

    public static CreateEmployeeCommand comandoCrear(boolean emailVerified) {
        return new CreateEmployeeCommand("VV-MARIANA", "Temporal123*", "Mariana Rojas",
                "mariana@vetrina.co", COMPANY_ID, emailVerified);
    }

    public static UpdateEmployeeCommand comandoActualizar() {
        return new UpdateEmployeeCommand(EMPLOYEE_ID, "VV-NUEVO", "Mariana Rojas Perez",
                "mariana.rojas@vetrina.co");
    }
}
