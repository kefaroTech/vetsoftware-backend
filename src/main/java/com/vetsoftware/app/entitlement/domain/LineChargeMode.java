package com.vetsoftware.app.entitlement.domain;

/**
 * Como cobra <strong>una linea concreta</strong> del contrato. Companion VO de
 * esta feature: el dominio de {@code companytrialgrant} no se importa.
 *
 * <p>
 * <strong>El estado del contrato deja de decidir el cobro.</strong> Un mismo
 * contrato lleva a la vez lineas en prueba y lineas de pago obligatorio, asi
 * que {@code TRIALING} ya no significa "a este cliente no se le cobra"
 * (R-TRIAL-13). Y el vencimiento barre por la fecha de cada linea, no por el
 * estado del contrato: un dia de mora no puede matar la prueba para siempre
 * (R-TRIAL-15).
 *
 * <p>
 * El precio real sigue guardado dentro de la linea gratuita, que es lo que hace
 * peligrosa la omision: una consulta que filtre solo por vigencia y olvide el
 * modo le cobra la cuota entera a todos los clientes en prueba (R-TRIAL-14).
 */
public enum LineChargeMode {

    /** Gratis, con caducidad. */
    TRIAL,

    /** Se cobra. Es el unico modo que devenga. */
    PAID,

    /** Gratis para siempre, pero con techo. */
    FREE_LIMITED,

    /**
     * La prueba vencio y el articulo quedo en consulta. Dieciocho caracteres: la
     * columna necesita veinte, y con quince este desenlace seria inescribible y el
     * barrido nocturno fallaria sobre todos los clientes a la vez (R-TRIAL-08).
     */
    EXPIRED_READ_ONLY;

    /** Solo un modo genera cargo. Los otros tres, jamas. */
    public boolean generatesCharge() {
        return this == PAID;
    }

    /** Caduca sola a la fecha: es la unica que necesita fila sucesora. */
    public boolean isTrial() {
        return this == TRIAL;
    }

    /** El origen que se escribe en el permiso de una linea que ya no es prueba. */
    public EntitlementSource entitlementSource(boolean core) {
        return switch (this) {
            case TRIAL -> EntitlementSource.TRIAL;
            case FREE_LIMITED -> EntitlementSource.FREE_LIMITED;
            case EXPIRED_READ_ONLY -> EntitlementSource.EXPIRED_TRIAL;
            case PAID -> core ? EntitlementSource.CORE : EntitlementSource.SUBSCRIPTION;
        };
    }

    /** El techo propio del modo: solo el desenlace de consulta baja de FULL. */
    public AccessLevel accessLevel() {
        return this == EXPIRED_READ_ONLY ? AccessLevel.READ_ONLY : AccessLevel.FULL;
    }
}
