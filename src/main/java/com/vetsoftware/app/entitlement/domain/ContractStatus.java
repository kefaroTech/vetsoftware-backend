package com.vetsoftware.app.entitlement.domain;

/**
 * Estado del contrato tal como lo necesita este slice. Es un companion del
 * estado que vive en {@code subscriptions.status}: se copia aqui en vez de
 * importarse porque el vertical slicing prohibe cruzar dominios.
 */
public enum ContractStatus {
    TRIALING, ACTIVE, PAST_DUE, READ_ONLY, CANCELLED, EXPIRED;

    /**
     * <strong>"Vigente" no es "sin fecha de fin" ni "status = ACTIVE".</strong> Un
     * contrato en {@code PAST_DUE} sigue siendo el contrato vigente de esa empresa
     * --debe, pero sigue trabajando-- y uno en {@code READ_ONLY} tambien. Los que
     * salen son {@code CANCELLED} y {@code EXPIRED}. Con el criterio equivocado el
     * error es invisible hasta que un cliente reclama.
     */
    public boolean isCurrent() {
        return this == TRIALING || this == ACTIVE || this == PAST_DUE || this == READ_ONLY;
    }

    /**
     * Techo de acceso que impone el estado del contrato. Nunca baja de
     * {@link AccessLevel#READ_ONLY}: ni la cancelacion corta el acceso a lo que el
     * cliente ya escribio.
     */
    public AccessLevel maxAccessLevel() {
        return switch (this) {
            case TRIALING, ACTIVE, PAST_DUE -> AccessLevel.FULL;
            case READ_ONLY, CANCELLED, EXPIRED -> AccessLevel.READ_ONLY;
        };
    }
}
