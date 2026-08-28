package com.vetsoftware.app.smmlvvalue.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El salario minimo legal mensual de <strong>un ano concreto</strong>, con la
 * norma que lo fijo y —esto es lo que la separa de su gemela {@code UvtValue}—
 * <strong>con su estado</strong>.
 *
 * <p>
 * <strong>Que una cifra este judicialmente en disputa es un dato, no una
 * nota.</strong> El decreto que fijo el salario de 2026 quedo suspendido
 * provisionalmente por el Consejo de Estado en febrero de 2026 y la fila siguio
 * existiendo, porque el Gobierno mantuvo el incremento por decreto transitorio.
 * Un sistema que solo guardara «1.750.905» no podria decirle a quien liquida
 * una sancion que ese numero esta pendiente de un fallo; con
 * {@link #getStatus()} en la fila, cualquier operacion que use la cifra puede
 * saberlo y decirlo.
 *
 * <p>
 * <strong>Lleva {@code @Version} en su entidad JPA</strong> —al contrario que
 * {@code uvt_values}, {@code vat_filing_periods} y {@code public_holidays}—
 * precisamente por eso: la suspension se anota <em>sobre la fila que ya
 * existia</em>. Es una mutacion declarada, y donde hay mutacion hay dos
 * operadores que pueden llegar a la vez.
 *
 * <p>
 * Como su gemela, el ano es parte del dato: no hay «el salario vigente».
 */
public class SmmlvValue {

    /** Espejo de {@code chk_smmlv_values_year}. */
    public static final int MIN_YEAR = 2020;

    /** Espejo de {@code chk_smmlv_values_year}. */
    public static final int MAX_YEAR = 2100;

    private static final int MAX_TEXTO = 255;

    private final Long id;
    private final int fiscalYear;
    private final BigDecimal valueAmount;
    private final String legalReference;
    private SmmlvStatus status;
    private String statusReference;
    private LocalDate statusChangedOn;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private Long version;

    public SmmlvValue(Long id, int fiscalYear, BigDecimal valueAmount, String legalReference,
            SmmlvStatus status, String statusReference, LocalDate statusChangedOn,
            LocalDateTime createdDate, boolean enabled, Long version) {
        if (fiscalYear < MIN_YEAR || fiscalYear > MAX_YEAR) {
            throw new IllegalArgumentException(
                    "fiscalYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (valueAmount == null || valueAmount.signum() <= 0) {
            throw new IllegalArgumentException("valueAmount must be greater than zero");
        }
        if (legalReference == null || legalReference.isBlank()) {
            throw new IllegalArgumentException("legalReference is required");
        }
        if (legalReference.length() > MAX_TEXTO) {
            throw new IllegalArgumentException(
                    "legalReference must be " + MAX_TEXTO + " chars or less");
        }
        validarEstado(status, statusReference, statusChangedOn);
        this.id = id;
        this.fiscalYear = fiscalYear;
        this.valueAmount = valueAmount;
        this.legalReference = legalReference;
        this.status = status;
        this.statusReference = statusReference;
        this.statusChangedOn = statusChangedOn;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /** Alta de un ano nuevo: siempre nace vigente y sin motivo de estado. */
    public static SmmlvValue create(int fiscalYear, BigDecimal valueAmount, String legalReference,
            LocalDateTime createdDate) {
        return new SmmlvValue(null, fiscalYear, valueAmount, legalReference, SmmlvStatus.IN_FORCE,
                null, null, createdDate, true, null);
    }

    /**
     * Anota el desenlace judicial o normativo sobre la fila que ya existe.
     *
     * <p>
     * Volver a {@link SmmlvStatus#IN_FORCE} <strong>limpia</strong> el motivo y la
     * fecha, porque {@code chk_smmlv_values_status} exige que sean nulos
     * exactamente cuando el estado es vigente: dejarlos puestos haria que la base
     * rechazara el {@code UPDATE} con un error que no nombra la columna.
     */
    public void changeStatus(SmmlvStatus nuevoEstado, String motivo, LocalDate fechaDelCambio) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (nuevoEstado == this.status) {
            throw new SmmlvStatusAlreadySetException(fiscalYear, nuevoEstado);
        }
        String motivoEfectivo = nuevoEstado == SmmlvStatus.IN_FORCE ? null : motivo;
        LocalDate fechaEfectiva = nuevoEstado == SmmlvStatus.IN_FORCE ? null : fechaDelCambio;
        validarEstado(nuevoEstado, motivoEfectivo, fechaEfectiva);
        this.status = nuevoEstado;
        this.statusReference = motivoEfectivo;
        this.statusChangedOn = fechaEfectiva;
    }

    /**
     * {@code true} si la cifra se puede usar sin advertencia. Lo consulta cualquier
     * calculo que dependa del salario minimo para decir, con la cifra, que esta en
     * disputa.
     */
    public boolean isInForce() {
        return status == SmmlvStatus.IN_FORCE;
    }

    private static void validarEstado(SmmlvStatus status, String statusReference,
            LocalDate statusChangedOn) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (status == SmmlvStatus.IN_FORCE) {
            if (statusReference != null || statusChangedOn != null) {
                throw new IllegalArgumentException(
                        "an in-force value cannot carry statusReference or statusChangedOn");
            }
            return;
        }
        if (statusReference == null || statusReference.isBlank()) {
            throw new IllegalArgumentException(
                    "statusReference is required when the status is not IN_FORCE");
        }
        if (statusReference.length() > MAX_TEXTO) {
            throw new IllegalArgumentException(
                    "statusReference must be " + MAX_TEXTO + " chars or less");
        }
        if (statusChangedOn == null) {
            throw new IllegalArgumentException(
                    "statusChangedOn is required when the status is not IN_FORCE");
        }
    }

    public Long getId() {
        return id;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public SmmlvStatus getStatus() {
        return status;
    }

    /** La providencia o la norma que movio el estado. Nula solo si esta vigente. */
    public String getStatusReference() {
        return statusReference;
    }

    public LocalDate getStatusChangedOn() {
        return statusChangedOn;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getVersion() {
        return version;
    }
}
