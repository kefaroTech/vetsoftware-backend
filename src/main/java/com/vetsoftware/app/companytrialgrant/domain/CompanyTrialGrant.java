package com.vetsoftware.app.companytrialgrant.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Qué probó ya esta empresa, y hasta cuándo. Una fila por empresa y artículo,
 * <strong>para siempre</strong>.
 *
 * <p>
 * <strong>Un artículo no se regala dos veces a la misma empresa,
 * jamás.</strong> Lo impone {@code uq_company_trial_grants_item}, y es la
 * invariante de la tabla, no una optimización: cierra de un golpe las cinco
 * vías de reinicio —reponer un módulo, cambiar de ciclo, migrar de tarifa,
 * reactivar tras suspender y recontratar tras cancelar—. Sin ella, quitar un
 * módulo el día 29 y reponerlo el 30 es software gratis indefinido y ninguna
 * fila del modelo estaría mal. El código <strong>no</strong> intenta sortearla:
 * reponer un módulo reusa la concesión que ya existe, con sus días restantes.
 *
 * <p>
 * <strong>La fecha de fin no se elige: se calcula, y la base lo
 * verifica.</strong> Es el menor entre «alta más sus días, con el último
 * incluido» y «fin de ventana». Inventario añadido el día 15 de una ventana de
 * 30 sale el día 30 de la ventana —15 días, no 30—; Caja con 14 días de
 * política concedida el día 0 vence a los 14 días contados desde su alta, antes
 * que la ventana. No se mueve nunca más.
 *
 * <p>
 * <strong>No se puede desconceder.</strong> La tabla no lleva {@code enabled} y
 * esta clase no expone borrado ni desactivación: ese es exactamente el punto de
 * la tabla. Lo que sí tiene es un desenlace, que se escribe una vez.
 */
public class CompanyTrialGrant {

    private final Long id;
    private final Long companyId;
    private final Long catalogItemId;
    private final Long trialWindowId;
    private final LocalDate trialWindowEndDate;
    private final LocalDate grantedOn;
    private final int daysGranted;
    private final LocalDate trialEndDate;
    private final int policyTrialDays;
    private final TrialPolicyOutcome policyTrialOutcome;
    private final Long sourceQuoteId;
    private final Long grantingAmendmentId;
    private final LocalDateTime consumedAt;
    private final TrialOutcome outcome;
    private final LocalDateTime createdDate;
    private final Long version;

    public CompanyTrialGrant(Long id, Long companyId, Long catalogItemId, Long trialWindowId,
            LocalDate trialWindowEndDate, LocalDate grantedOn, int daysGranted,
            LocalDate trialEndDate, int policyTrialDays, TrialPolicyOutcome policyTrialOutcome,
            Long sourceQuoteId, Long grantingAmendmentId, LocalDateTime consumedAt,
            TrialOutcome outcome, LocalDateTime createdDate, Long version) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalog item id is required");
        if (trialWindowId == null)
            throw new IllegalArgumentException("trial window id is required");
        if (trialWindowEndDate == null)
            throw new IllegalArgumentException("trial window end date is required");
        if (grantedOn == null)
            throw new IllegalArgumentException("granted on is required");
        if (policyTrialOutcome == null)
            throw new IllegalArgumentException("policy trial outcome is required");
        // chk_company_trial_grants_days
        if (daysGranted <= 0)
            throw new IllegalArgumentException("days granted must be greater than zero");
        if (policyTrialDays <= 0)
            throw new IllegalArgumentException("policy trial days must be greater than zero");
        if (daysGranted > policyTrialDays)
            throw new IllegalArgumentException("days granted (" + daysGranted
                    + ") cannot exceed the frozen policy of " + policyTrialDays + " days:"
                    + " nobody trials longer than the catalog allows");
        // chk_company_trial_grants_within_window
        if (grantedOn.isAfter(trialWindowEndDate))
            throw new IllegalArgumentException(
                    "granted on " + grantedOn + " is past the window end " + trialWindowEndDate);
        // chk_company_trial_grants_end
        LocalDate expectedEnd = endDateFor(grantedOn, daysGranted, trialWindowEndDate);
        if (!expectedEnd.equals(trialEndDate))
            throw new IllegalArgumentException("trial end date must be " + expectedEnd
                    + " (the earlier of granted_on + days - 1 and the window end) but was "
                    + trialEndDate);
        // chk_company_trial_grants_paper: exactamente un papel concedió la prueba.
        if ((sourceQuoteId == null) == (grantingAmendmentId == null))
            throw new IllegalArgumentException("exactly one of source quote or granting amendment"
                    + " must be set: a trial without paper cannot be defended");
        // chk_company_trial_grants_outcome
        if (consumedAt == null && outcome != null)
            throw new IllegalArgumentException(
                    "an outcome without a consumed date is not a result");
        if (consumedAt != null && outcome == null)
            throw new IllegalArgumentException("a consumed trial must record how it ended");
        this.id = id;
        this.companyId = companyId;
        this.catalogItemId = catalogItemId;
        this.trialWindowId = trialWindowId;
        this.trialWindowEndDate = trialWindowEndDate;
        this.grantedOn = grantedOn;
        this.daysGranted = daysGranted;
        this.trialEndDate = trialEndDate;
        this.policyTrialDays = policyTrialDays;
        this.policyTrialOutcome = policyTrialOutcome;
        this.sourceQuoteId = sourceQuoteId;
        this.grantingAmendmentId = grantingAmendmentId;
        this.consumedAt = consumedAt;
        this.outcome = outcome;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * <strong>La cuenta de los días heredados.</strong> El fin de una prueba es el
     * menor entre «alta más sus días, con el último incluido» y el fin de la
     * ventana. La ventana no se estira nunca, así que un módulo añadido a mitad de
     * ventana recibe lo que queda y no sus días completos.
     *
     * <p>
     * Es la misma expresión que {@code chk_company_trial_grants_end} comprueba en
     * la fila. Están escritas dos veces a propósito —una en el motor y otra aquí— y
     * tienen que decir lo mismo: si divergen, el alta comercial muere en la base
     * con un mensaje que no señala a la causa.
     */
    public static LocalDate endDateFor(LocalDate grantedOn, int days, LocalDate windowEndDate) {
        if (grantedOn == null)
            throw new IllegalArgumentException("granted on is required");
        if (windowEndDate == null)
            throw new IllegalArgumentException("window end date is required");
        if (days <= 0)
            throw new IllegalArgumentException("days must be greater than zero");
        LocalDate byPolicy = grantedOn.plusDays(days - 1L);
        return byPolicy.isBefore(windowEndDate) ? byPolicy : windowEndDate;
    }

    /**
     * Concede la prueba de un artículo dentro de una ventana.
     *
     * @param daysGranted
     *            los días que la oferta concede. Puede ser menor que la política
     *            —una campaña puede bajar de ahí— pero nunca mayor. Lo que recorta
     *            el fin no es este número sino la ventana.
     */
    public static CompanyTrialGrant grant(TrialWindowRef window, Long catalogItemId,
            LocalDate grantedOn, int daysGranted, int policyTrialDays,
            TrialPolicyOutcome policyTrialOutcome, Long sourceQuoteId, Long grantingAmendmentId,
            LocalDateTime createdDate) {
        if (window == null)
            throw new IllegalArgumentException("trial window is required");
        if (!window.admitsGrantOn(grantedOn))
            throw new TrialWindowNotOpenException(window.companyId(), grantedOn, window.endDate());
        return new CompanyTrialGrant(null, window.companyId(), catalogItemId, window.id(),
                window.endDate(), grantedOn, daysGranted,
                endDateFor(grantedOn, daysGranted, window.endDate()), policyTrialDays,
                policyTrialOutcome, sourceQuoteId, grantingAmendmentId, null, null, createdDate,
                null);
    }

    /**
     * Resuelve la prueba: escribe cuándo acabó y cómo. No edita ninguna de las
     * fechas concedidas.
     *
     * <p>
     * Se resuelve una sola vez. Volver a resolverla movería la fecha con la que se
     * calcula la tasa de conversión, y esa cifra es la que decide qué duración de
     * campaña se repite.
     */
    public CompanyTrialGrant consume(LocalDateTime at, TrialOutcome resolvedOutcome) {
        if (at == null)
            throw new IllegalArgumentException("consumed at is required");
        if (resolvedOutcome == null)
            throw new IllegalArgumentException("outcome is required");
        if (consumedAt != null)
            throw new TrialAlreadyConsumedException(companyId, catalogItemId, consumedAt);
        return new CompanyTrialGrant(id, companyId, catalogItemId, trialWindowId,
                trialWindowEndDate, grantedOn, daysGranted, trialEndDate, policyTrialDays,
                policyTrialOutcome, sourceQuoteId, grantingAmendmentId, at, resolvedOutcome,
                createdDate, version);
    }

    /** Vacío en {@code consumedAt} = prueba viva. */
    public boolean isLive() {
        return consumedAt == null;
    }

    /**
     * Los días que la empresa acaba probando de verdad, tras el recorte de la
     * ventana. Es el número que hay que enseñar: «le quedan 8», no «tiene 30».
     */
    public int effectiveDays() {
        return (int) ChronoUnit.DAYS.between(grantedOn, trialEndDate) + 1;
    }

    /**
     * Si la prueba sigue viva ese día. El último día es inclusivo: una prueba que
     * termina el 30 de septiembre sigue viva a las 19:30 de ese día.
     */
    public boolean isActiveOn(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("day is required");
        return isLive() && !day.isBefore(grantedOn) && !day.isAfter(trialEndDate);
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public Long getTrialWindowId() {
        return trialWindowId;
    }

    public LocalDate getTrialWindowEndDate() {
        return trialWindowEndDate;
    }

    public LocalDate getGrantedOn() {
        return grantedOn;
    }

    public int getDaysGranted() {
        return daysGranted;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public int getPolicyTrialDays() {
        return policyTrialDays;
    }

    public TrialPolicyOutcome getPolicyTrialOutcome() {
        return policyTrialOutcome;
    }

    public Long getSourceQuoteId() {
        return sourceQuoteId;
    }

    public Long getGrantingAmendmentId() {
        return grantingAmendmentId;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public TrialOutcome getOutcome() {
        return outcome;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
