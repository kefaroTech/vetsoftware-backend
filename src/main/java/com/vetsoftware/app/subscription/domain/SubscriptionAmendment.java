package com.vetsoftware.app.subscription.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * El otrosi: el papel de cada cambio del contrato. <strong>Documento
 * inmutable</strong> — corregir un otrosi es emitir otro, nunca editarlo, y por
 * eso la tabla no lleva {@code version} ({@code E1_APPEND_ONLY}) ni
 * {@code enabled}.
 *
 * <p>
 * Sin esta clase se veria el estado final del contrato y no la pelicula de como
 * se llego ahi, que es exactamente lo que hace imposible auditar una factura
 * discutida seis meses despues.
 *
 * <p>
 * <strong>{@code proration_amount} y {@code monthly_delta_amount} los calcula
 * el servidor</strong>, en {@link ProrationCalculator}, a partir de la fecha
 * efectiva contra el periodo de facturacion en curso. Hasta la incidencia #386
 * llegaban en el cuerpo de la peticion y se persistian tal cual —el importe lo
 * dictaba quien mandaba la peticion—; hoy ningun caller puede fijarlos, porque
 * los commands ya no los transportan. Los dos siguen yendo <strong>con
 * signo</strong>: una baja resta (§3.1 de {@code suscripciones-modelo.md}).
 *
 * <p>
 * Lo que sigue sin estar especificado en el modelo, y este slice no inventa: la
 * mora y las notas credito.
 */
public class SubscriptionAmendment {

    private static final int MAX_NUMBER_LENGTH = 30;
    private static final int MAX_REASON_LENGTH = 255;
    private static final int MAX_CLIENT_REQUEST_LENGTH = 64;
    private static final int MAX_AMOUNT_SCALE = 2;

    private final Long id;
    private final Long companyId;
    private final Long subscriptionId;
    private final String amendmentNumber;
    private final AmendmentType amendmentType;
    private final LocalDate effectiveDate;
    private final String reason;
    private final Long requestedByEmployeeId;
    private final Long requestedBySystemUserId;
    private final BigDecimal prorationAmount;
    private final BigDecimal monthlyDeltaAmount;
    private final Long quoteId;
    private final String clientRequestId;
    private final LocalDateTime createdDate;

    public SubscriptionAmendment(Long id, Long companyId, Long subscriptionId,
            String amendmentNumber, AmendmentType amendmentType, LocalDate effectiveDate,
            String reason, Long requestedByEmployeeId, Long requestedBySystemUserId,
            BigDecimal prorationAmount, BigDecimal monthlyDeltaAmount, Long quoteId,
            String clientRequestId, LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (amendmentNumber == null || amendmentNumber.isBlank())
            throw new IllegalArgumentException("amendmentNumber is required");
        if (amendmentNumber.length() > MAX_NUMBER_LENGTH)
            throw new IllegalArgumentException(
                    "amendmentNumber must be " + MAX_NUMBER_LENGTH + " chars or less");
        if (amendmentType == null)
            throw new IllegalArgumentException("amendmentType is required");
        if (effectiveDate == null)
            throw new IllegalArgumentException("effectiveDate is required");
        if (reason != null && reason.length() > MAX_REASON_LENGTH)
            throw new IllegalArgumentException(
                    "reason must be " + MAX_REASON_LENGTH + " chars or less");
        // chk_subscription_amendments_actor: exclusividad mutua Y obligatoriedad. Un
        // cambio de contrato sin responsable es un cambio que nadie firmo.
        boolean byEmployee = requestedByEmployeeId != null;
        boolean bySystemUser = requestedBySystemUserId != null;
        if (byEmployee == bySystemUser)
            throw new IllegalArgumentException(
                    "an amendment needs exactly one requester: employee or system user");
        requireAmount(prorationAmount, "prorationAmount");
        requireAmount(monthlyDeltaAmount, "monthlyDeltaAmount");
        if (clientRequestId == null || clientRequestId.isBlank())
            throw new IllegalArgumentException("clientRequestId is required");
        if (clientRequestId.length() > MAX_CLIENT_REQUEST_LENGTH)
            throw new IllegalArgumentException(
                    "clientRequestId must be " + MAX_CLIENT_REQUEST_LENGTH + " chars or less");
        this.id = id;
        this.companyId = companyId;
        this.subscriptionId = subscriptionId;
        this.amendmentNumber = amendmentNumber;
        this.amendmentType = amendmentType;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
        this.requestedByEmployeeId = requestedByEmployeeId;
        this.requestedBySystemUserId = requestedBySystemUserId;
        this.prorationAmount = prorationAmount;
        this.monthlyDeltaAmount = monthlyDeltaAmount;
        this.quoteId = quoteId;
        this.clientRequestId = clientRequestId;
        this.createdDate = createdDate;
    }

    public static SubscriptionAmendment issue(Long companyId, Long subscriptionId,
            String amendmentNumber, AmendmentType amendmentType, LocalDate effectiveDate,
            String reason, Long requestedByEmployeeId, Long requestedBySystemUserId,
            BigDecimal prorationAmount, BigDecimal monthlyDeltaAmount, Long quoteId,
            String clientRequestId) {
        return new SubscriptionAmendment(null, companyId, subscriptionId, amendmentNumber,
                amendmentType, effectiveDate, reason, requestedByEmployeeId,
                requestedBySystemUserId, prorationAmount, monthlyDeltaAmount, quoteId,
                clientRequestId, null);
    }

    /**
     * Los dos importes van con signo —una baja resta— asi que no se valida el
     * sentido, solo que exista y tenga la escala de {@code DECIMAL(19,2)}. Quien
     * decide el valor es {@link ProrationCalculator}, no el llamante.
     */
    private static void requireAmount(BigDecimal amount, String field) {
        if (amount == null)
            throw new IllegalArgumentException(field + " is required");
        if (amount.scale() > MAX_AMOUNT_SCALE)
            throw new IllegalArgumentException(field + " must have at most two decimals");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public String getAmendmentNumber() {
        return amendmentNumber;
    }

    public AmendmentType getAmendmentType() {
        return amendmentType;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public String getReason() {
        return reason;
    }

    public Long getRequestedByEmployeeId() {
        return requestedByEmployeeId;
    }

    public Long getRequestedBySystemUserId() {
        return requestedBySystemUserId;
    }

    public BigDecimal getProrationAmount() {
        return prorationAmount;
    }

    public BigDecimal getMonthlyDeltaAmount() {
        return monthlyDeltaAmount;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
