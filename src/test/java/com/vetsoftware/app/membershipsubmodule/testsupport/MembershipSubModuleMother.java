package com.vetsoftware.app.membershipsubmodule.testsupport;

import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo membershipsubmodule.
 *
 * <p>
 * Los enlaces se construyen con el constructor publico y no con
 * {@code MembershipSubModule.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class MembershipSubModuleMother {

    public static final Long RELATION_ID = 500L;
    public static final Long MEMBERSHIP_ID = 900L;
    public static final Long SUB_MODULE_ID = 980L;
    public static final Long OTRO_MEMBERSHIP_ID = 901L;
    public static final Long OTRO_SUB_MODULE_ID = 981L;

    public static final MembershipRef PLAN_PREMIUM = new MembershipRef(MEMBERSHIP_ID,
            "Plan Premium");
    public static final SubModuleRef FACTURACION = new SubModuleRef(SUB_MODULE_ID, "Facturacion",
            "FACT");

    public static final MembershipRef OTRO_PLAN = new MembershipRef(OTRO_MEMBERSHIP_ID,
            "Plan Basico");
    public static final SubModuleRef INVENTARIO = new SubModuleRef(OTRO_SUB_MODULE_ID, "Inventario",
            "INV");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private MembershipSubModuleMother() {
    }

    /** Enlace activo, habilitado. El caso por defecto. */
    public static MembershipSubModule activa() {
        return activa(RELATION_ID);
    }

    public static MembershipSubModule activa(Long id) {
        return new MembershipSubModule(id, PLAN_PREMIUM, FACTURACION, CREADO, true);
    }

    public static MembershipSubModule deshabilitada() {
        return new MembershipSubModule(RELATION_ID, PLAN_PREMIUM, FACTURACION, CREADO, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateMembershipSubModuleCommand comandoCrear() {
        return new CreateMembershipSubModuleCommand(MEMBERSHIP_ID, SUB_MODULE_ID);
    }

    /** Comando de actualizacion que cambia membresia y submodulo. */
    public static UpdateMembershipSubModuleCommand comandoActualizar() {
        return new UpdateMembershipSubModuleCommand(RELATION_ID, OTRO_MEMBERSHIP_ID,
                OTRO_SUB_MODULE_ID);
    }
}
