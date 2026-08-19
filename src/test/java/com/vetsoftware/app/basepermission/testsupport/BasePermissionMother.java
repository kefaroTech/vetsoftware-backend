package com.vetsoftware.app.basepermission.testsupport;

import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.command.UpdateBasePermissionCommand;
import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo basepermission.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code BasePermission.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class BasePermissionMother {

    public static final Long BASE_PERMISSION_ID = 100L;

    public static final SubModuleRef VENTAS = new SubModuleRef(1L, "Ventas", "VEN");
    public static final SubModuleRef INVENTARIO = new SubModuleRef(2L, "Inventario", "INV");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private BasePermissionMother() {
    }

    /** Permiso habilitado del submodulo Ventas. El caso por defecto. */
    public static BasePermission crearFactura() {
        return crearFactura(BASE_PERMISSION_ID);
    }

    public static BasePermission crearFactura(Long id) {
        return new BasePermission(id, "Crear factura", "INVOICE_CREATE", VENTAS, CREADO, null,
                true);
    }

    public static BasePermission deshabilitado() {
        return new BasePermission(BASE_PERMISSION_ID, "Crear factura", "INVOICE_CREATE", VENTAS,
                CREADO, null, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateBasePermissionCommand comandoCrear() {
        return new CreateBasePermissionCommand("Crear factura", "INVOICE_CREATE", VENTAS.id());
    }

    /** Comando de actualizacion que cambia nombre, codigo y submodulo. */
    public static UpdateBasePermissionCommand comandoActualizar() {
        return new UpdateBasePermissionCommand(BASE_PERMISSION_ID, "Editar factura",
                "INVOICE_UPDATE", INVENTARIO.id());
    }
}
