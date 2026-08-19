package com.vetsoftware.app.membership.testsupport;

import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo membership.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Membership.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class MembershipMother {

    public static final Long MEMBERSHIP_ID = 100L;

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private MembershipMother() {
    }

    /** Membresia activa, habilitada, no obligatoria. El caso por defecto. */
    public static Membership activa() {
        return activa(MEMBERSHIP_ID);
    }

    public static Membership activa(Long id) {
        return new Membership(id, "Plan Oro", MembershipStatus.ACTIVE, false, CREADO, null, true);
    }

    public static Membership obligatoria() {
        return new Membership(MEMBERSHIP_ID, "Plan Oro", MembershipStatus.ACTIVE, true, CREADO,
                null, true);
    }

    public static Membership deshabilitada() {
        return new Membership(MEMBERSHIP_ID, "Plan Oro", MembershipStatus.ACTIVE, false, CREADO,
                null, false);
    }

    public static Membership conEstado(MembershipStatus status) {
        return new Membership(MEMBERSHIP_ID, "Plan Oro", status, false, CREADO, null, true);
    }

    /** Comando de creacion coherente con la fixture de arriba. */
    public static CreateMembershipCommand comandoCrear() {
        return new CreateMembershipCommand("Plan Oro", "ACTIVE", false);
    }

    /** Comando de actualizacion que cambia nombre, estado y obligatoriedad. */
    public static UpdateMembershipCommand comandoActualizar() {
        return new UpdateMembershipCommand(MEMBERSHIP_ID, "Plan Platino", "DEPRECATED", true);
    }
}
