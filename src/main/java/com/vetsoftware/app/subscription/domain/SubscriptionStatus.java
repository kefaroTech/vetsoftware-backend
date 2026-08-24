package com.vetsoftware.app.subscription.domain;

import java.util.Set;

/**
 * Los seis estados de un contrato, y la politica que impide que haya un
 * septimo.
 *
 * <p>
 * <strong>R18 · no existe, ni debe implementarse, un estado de corte total de
 * acceso.</strong> El maximo de restriccion es {@link #READ_ONLY}: el cliente
 * moroso puede consultar e imprimir su historia clinica, y no puede crear ni
 * modificar. Un cliente que se queda sin poder leer lo que es legalmente suyo
 * es una reclamacion garantizada y un riesgo legal real, asi que la politica no
 * vive en un documento sino aqui: {@link #allowsRead()} devuelve {@code true}
 * para <em>todos</em> los valores, y anadir un estado que devuelva
 * {@code false} rompe el test de dominio que lo comprueba constante a
 * constante.
 *
 * <p>
 * {@link #PAST_DUE} es la gracia: debe, pero sigue trabajando. Es un estado
 * <em>vigente</em>, no una suspension.
 */
public enum SubscriptionStatus {

    /** Periodo de prueba. Vigente. */
    TRIALING(true, true),
    /** Al dia. Vigente. */
    ACTIVE(true, true),
    /** Debe y sigue trabajando: esto es la gracia, no una suspension. Vigente. */
    PAST_DUE(true, true),
    /** Solo consulta. El maximo de restriccion que admite el producto. Vigente. */
    READ_ONLY(true, false),
    /** El cliente se fue. Ya no es el contrato vigente de la empresa. */
    CANCELLED(false, false),
    /** Vencido sin renovar. Ya no es el contrato vigente de la empresa. */
    EXPIRED(false, false);

    /**
     * Los estados que cuentan como «contrato vigente de esta empresa» y que
     * alimentan la columna generada {@code active_marker}, sobre la que descansa
     * {@code uq_subscriptions_active_company}. <strong>Escrito una sola
     * vez</strong>: si el criterio se duplica, la version que se olvide de
     * {@code PAST_DUE} deja a un moroso sin permisos y nadie lo nota hasta que
     * reclama.
     */
    public static final Set<SubscriptionStatus> CURRENT = Set.of(TRIALING, ACTIVE, PAST_DUE,
            READ_ONLY);

    private final boolean current;
    private final boolean write;

    SubscriptionStatus(boolean current, boolean write) {
        this.current = current;
        this.write = write;
    }

    /** ¿Ocupa el marcador de contrato vigente de su empresa? */
    public boolean isCurrent() {
        return current;
    }

    /** ¿Puede crear y modificar informacion en este estado? */
    public boolean allowsWrite() {
        return write;
    }

    /**
     * Siempre {@code true}, en los seis estados y sin excepcion. Es R18 escrita
     * como codigo: no hay ningun estado del contrato que le quite a un cliente el
     * acceso de lectura a su propia informacion.
     */
    public boolean allowsRead() {
        return true;
    }

    /** Estados terminales: de aqui no se sale. */
    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED;
    }
}
