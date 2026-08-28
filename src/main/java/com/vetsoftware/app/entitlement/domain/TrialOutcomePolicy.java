package com.vetsoftware.app.entitlement.domain;

/**
 * Que pasa el dia siguiente al vencimiento de una prueba, <strong>congelado en
 * la concesion el dia que se concedio</strong>. Companion VO: espeja
 * {@code company_trial_grants.policy_trial_outcome} sin importar el dominio de
 * {@code companytrialgrant}.
 *
 * <p>
 * Aqui vive la mitad de la traduccion entre los tres vocabularios que le toca a
 * esta feature (R-TRIAL-20). Son tres conceptos distintos --politica, modo de
 * cobro y resultado-- y por eso no comparten nombre: tenerlos con nombres
 * parecidos y traducidos en tres sitios es exactamente como un desenlace acaba
 * produciendo la terna de otro.
 *
 * <table border="1">
 * <caption>La terna de cada politica</caption>
 * <tr>
 * <th>Politica</th>
 * <th>Modo de la linea sucesora</th>
 * <th>Origen del permiso</th>
 * <th>Nivel</th>
 * </tr>
 * <tr>
 * <td>CONVERT_TO_PAID</td>
 * <td>PAID</td>
 * <td>SUBSCRIPTION</td>
 * <td>FULL</td>
 * </tr>
 * <tr>
 * <td>LIMITED</td>
 * <td>FREE_LIMITED</td>
 * <td>FREE_LIMITED</td>
 * <td>FULL</td>
 * </tr>
 * <tr>
 * <td>READ_ONLY</td>
 * <td>EXPIRED_READ_ONLY</td>
 * <td>EXPIRED_TRIAL</td>
 * <td>READ_ONLY</td>
 * </tr>
 * </table>
 */
public enum TrialOutcomePolicy {

    /** Empieza a cobrarse solo al vencer. */
    CONVERT_TO_PAID,

    /** Sigue usandose gratis, con el cupo recortado. */
    LIMITED,

    /** Solo consulta lo que ya cargo. */
    READ_ONLY;

    /** El modo de cobro que toma la linea sucesora. */
    public LineChargeMode successorChargeMode() {
        return switch (this) {
            case CONVERT_TO_PAID -> LineChargeMode.PAID;
            case LIMITED -> LineChargeMode.FREE_LIMITED;
            case READ_ONLY -> LineChargeMode.EXPIRED_READ_ONLY;
        };
    }

    /** El origen que se escribe en el permiso sucesor. */
    public EntitlementSource entitlementSource(boolean core) {
        return successorChargeMode().entitlementSource(core);
    }

    /** El nivel de acceso del permiso sucesor, antes del techo del contrato. */
    public AccessLevel accessLevel() {
        return successorChargeMode().accessLevel();
    }
}
