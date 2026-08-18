package com.vetsoftware.app.submodule.testsupport;

import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.command.UpdateSubModuleCommand;
import com.vetsoftware.app.submodule.domain.ModuleRef;
import com.vetsoftware.app.submodule.domain.SubModule;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo submodule.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code SubModule.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class SubModuleMother {

    public static final Long SUB_MODULE_ID = 100L;

    public static final ModuleRef FACTURACION = new ModuleRef(1L, "Facturacion", "FACT");
    public static final ModuleRef INVENTARIO = new ModuleRef(2L, "Inventario", "INV");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SubModuleMother() {
    }

    /** Submodulo habilitado del modulo Facturacion. El caso por defecto. */
    public static SubModule reportes() {
        return reportes(SUB_MODULE_ID);
    }

    public static SubModule reportes(Long id) {
        return new SubModule(id, "Reportes", "REP", FACTURACION, CREADO, true);
    }

    public static SubModule deshabilitado() {
        return new SubModule(SUB_MODULE_ID, "Reportes", "REP", FACTURACION, CREADO, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateSubModuleCommand comandoCrear() {
        return new CreateSubModuleCommand("Reportes", "REP", FACTURACION.id());
    }

    /** Comando de actualizacion que cambia nombre, codigo y modulo. */
    public static UpdateSubModuleCommand comandoActualizar() {
        return new UpdateSubModuleCommand(SUB_MODULE_ID, "Facturacion Avanzada", "FACT-AV",
                INVENTARIO.id());
    }
}
