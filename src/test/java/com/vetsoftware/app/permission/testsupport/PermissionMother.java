package com.vetsoftware.app.permission.testsupport;

import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.domain.CompanyRef;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.domain.SubModuleRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo permission.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Permission.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class PermissionMother {

    public static final Long PERMISSION_ID = 100L;
    public static final Long COMPANY_ID = 9L;
    public static final Long SUB_MODULE_ID = 5L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final SubModuleRef INVENTARIO = new SubModuleRef(SUB_MODULE_ID, "Inventario",
            "INV");

    public static final CompanyRef OTRA_CLINICA = new CompanyRef(10L, "Clinica Sur", "NIT-901");
    public static final SubModuleRef FACTURACION = new SubModuleRef(6L, "Facturacion", "FACT");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private PermissionMother() {
    }

    public static Permission permisoValido() {
        return permisoValido(PERMISSION_ID);
    }

    public static Permission permisoValido(Long id) {
        return new Permission(id, "Crear factura", "billing.create", CLINICA, INVENTARIO, CREADO,
                true);
    }

    public static Permission deshabilitado() {
        return new Permission(PERMISSION_ID, "Crear factura", "billing.create", CLINICA, INVENTARIO,
                CREADO, false);
    }

    public static CreatePermissionCommand comandoCrear() {
        return new CreatePermissionCommand("Crear factura", "billing.create", COMPANY_ID,
                SUB_MODULE_ID);
    }

    public static UpdatePermissionCommand comandoActualizar() {
        return new UpdatePermissionCommand(PERMISSION_ID, "Editar factura", "billing.update",
                COMPANY_ID, SUB_MODULE_ID);
    }
}
