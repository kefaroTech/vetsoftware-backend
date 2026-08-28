package com.vetsoftware.app.companytrialgrant.domain;

/**
 * Qué pasa el día siguiente al vencimiento de una prueba. Es la
 * <em>política</em> del artículo, congelada en la concesión el día que se
 * concedió.
 *
 * <p>
 * <strong>Aquí vive la traducción entre los tres vocabularios, escrita una sola
 * vez</strong> (R-TRIAL-20). Son tres conceptos distintos —política, modo de
 * cobro y resultado— y por eso no comparten nombre; tenerlos con nombres
 * parecidos y traducidos en tres sitios es exactamente cómo un desenlace acaba
 * produciendo la terna de otro:
 *
 * <table border="1">
 * <caption>La terna de cada política</caption>
 * <tr>
 * <th>Política</th>
 * <th>Modo de la línea</th>
 * <th>Origen del permiso</th>
 * <th>Nivel</th>
 * <th>Desenlace</th>
 * </tr>
 * <tr>
 * <td>CONVERT_TO_PAID</td>
 * <td>PAID</td>
 * <td>SUBSCRIPTION</td>
 * <td>FULL</td>
 * <td>CONVERTED</td>
 * </tr>
 * <tr>
 * <td>LIMITED</td>
 * <td>FREE_LIMITED</td>
 * <td>FREE_LIMITED</td>
 * <td>FULL</td>
 * <td>LIMITED</td>
 * </tr>
 * <tr>
 * <td>READ_ONLY</td>
 * <td>EXPIRED_READ_ONLY</td>
 * <td>EXPIRED_TRIAL</td>
 * <td>READ_ONLY</td>
 * <td>READ_ONLY</td>
 * </tr>
 * </table>
 *
 * <p>
 * Espeja {@code chk_company_trial_grants_policy_outcome}.
 */
public enum TrialPolicyOutcome {

    /** Empieza a cobrarse solo al vencer. */
    CONVERT_TO_PAID,

    /** Sigue usándose gratis, con el cupo recortado. */
    LIMITED,

    /** Solo consulta lo que ya cargó. */
    READ_ONLY;

    /**
     * El modo de cobro que toma la línea sucesora.
     *
     * <p>
     * El cuarto valor —{@code EXPIRED_READ_ONLY}, dieciocho caracteres— existe
     * porque el catálogo admite ese desenlace: sin él la línea sucesora quedaría
     * marcada como pagada sin que nadie la pague.
     */
    public TrialChargeMode chargeMode() {
        return switch (this) {
            case CONVERT_TO_PAID -> TrialChargeMode.PAID;
            case LIMITED -> TrialChargeMode.FREE_LIMITED;
            case READ_ONLY -> TrialChargeMode.EXPIRED_READ_ONLY;
        };
    }

    /** El origen que se escribe en el permiso sucesor. */
    public TrialEntitlementSource entitlementSource() {
        return switch (this) {
            case CONVERT_TO_PAID -> TrialEntitlementSource.SUBSCRIPTION;
            case LIMITED -> TrialEntitlementSource.FREE_LIMITED;
            case READ_ONLY -> TrialEntitlementSource.EXPIRED_TRIAL;
        };
    }

    /** El nivel de acceso del permiso sucesor. */
    public TrialAccessLevel accessLevel() {
        return this == READ_ONLY ? TrialAccessLevel.READ_ONLY : TrialAccessLevel.FULL;
    }

    /** Cómo queda marcada la concesión cuando vence en su fecha. */
    public TrialOutcome resolvedOutcome() {
        return switch (this) {
            case CONVERT_TO_PAID -> TrialOutcome.CONVERTED;
            case LIMITED -> TrialOutcome.LIMITED;
            case READ_ONLY -> TrialOutcome.READ_ONLY;
        };
    }
}
